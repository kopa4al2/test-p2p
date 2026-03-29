package com.app.ui

import com.app.common.ChatHistoryMessage
import com.app.common.ChatMessage
import com.app.common.PeerInfo
import com.app.common.config.ConfigManager
import com.app.common.db.DatabaseManager
import com.app.network.peer.PeerDiscoveryStrategy
import javafx.application.Platform
import javafx.collections.FXCollections
import javafx.collections.ObservableList
import kotlinx.coroutines.*
import org.slf4j.LoggerFactory
import org.stefan.chat.ChatPeer
import java.net.InetAddress
import java.util.*
import kotlin.time.Duration.Companion.milliseconds

class MainController(
    private val chatPeer: ChatPeer,
    private val discovery: PeerDiscoveryStrategy
) {
    private val logger = LoggerFactory.getLogger(MainController::class.java)
    val peers: ObservableList<PeerInfo> = FXCollections.observableArrayList()
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val offlineNotificationSent = mutableSetOf<String>()

    var onMessageReceived: ((ChatHistoryMessage) -> Unit)? = null
    var onHistoryUpdated: ((List<ChatHistoryMessage>) -> Unit)? = null
    var onPeerListRefresh: (() -> Unit)? = null
    var selectedPeer: PeerInfo? = null

    fun start() {
        DatabaseManager.init(ConfigManager.current.userId)
        loadKnownPeers()
        scope.launch {
            chatPeer.startServer { incomingMsg ->
                Platform.runLater {
                    handleIncomingMessage(incomingMsg)
                }
            }

            val port = chatPeer.tcpPort.await()
            logger.info("Server started on port: {}", port)
            discovery.startAdvertising(ConfigManager.current.userName, port)

            discovery.startScanning { peer ->
                handleDiscoveredPeer(peer)
            }
        }
        startOnlineStatusChecker()
    }

    private fun loadKnownPeers() {
        scope.launch {
            val knownPeers = DatabaseManager.getAllPeers()
            knownPeers.forEach { (id, name, lastSeen) ->
                if (peers.none { it.id == id }) {
                    peers.add(
                        PeerInfo(
                            id = id,
                            name = name,
                            address = InetAddress.getByName("0.0.0.0"),
                            tcpPort = 0,
                            lastSeen = lastSeen,
                            isOnline = false
                        )
                    )
                }
            }
        }
    }

    fun selectPeer(peer: PeerInfo?) {
        selectedPeer = peer
        if (peer != null) {
            peer.hasUnread = false
            offlineNotificationSent.remove(peer.id)
            onPeerListRefresh?.invoke()
            scope.launch {
                val history = DatabaseManager.getHistory(peer.id)
                onHistoryUpdated?.invoke(history)
            }
        } else {
            onHistoryUpdated?.invoke(emptyList())
        }
    }

    fun sendMessage(text: String) {
        val peer = selectedPeer ?: return
        val myId = discovery.instanceId
        val myName = ConfigManager.current.userName
        if (!peer.isOnline && !offlineNotificationSent.contains(peer.id)) {
            onMessageReceived?.invoke(
                ChatHistoryMessage(
                    messageId = UUID.randomUUID().toString(),
                    senderId = "system",
                    senderName = "System",
                    content = "Peer is currently offline. Message will be sent when they come online.",
                    timeSend = System.currentTimeMillis()
                )
            )
            offlineNotificationSent.add(peer.id)
        }
        val messageId = UUID.randomUUID().toString()
        val timestamp = System.currentTimeMillis()

        scope.launch {
            DatabaseManager.saveMessage(
                peerId = peer.id,
                senderId = myId,
                senderName = myName,
                content = text,
                timestamp = timestamp,
                messageId = messageId,
                isPending = true
            )

            if (peer.isOnline) {
                val success = chatPeer.sendMessage(
                    peer.address.hostAddress,
                    peer.tcpPort,
                    ChatMessage(
                        messageId,
                        myId,
                        myName,
                        text
                    )
                )
                if (success) {
                    DatabaseManager.markAsSent(messageId)
                    // Update the UI message status
                    onMessageReceived?.invoke(
                        ChatHistoryMessage(
                            messageId = messageId,
                            senderId = myId,
                            senderName = myName,
                            content = text,
                            timeSend = timestamp,
                            isPending = false
                        )
                    )
                }
            }
        }

        onMessageReceived?.invoke(
            ChatHistoryMessage(
                messageId = messageId,
                senderId = myId,
                senderName = myName,
                content = text,
                timeSend = timestamp,
                isPending = !peer.isOnline // Initial state in UI
            )
        )
    }

    private fun resendPendingMessages(peer: PeerInfo) {
        if (!peer.isOnline) return
        scope.launch {
            val pending = DatabaseManager.getPendingMessages().filter { it.first == peer.id }
            if (pending.isEmpty()) return@launch
            
            logger.info("Resending {} pending messages to {}", pending.size, peer.name)
            for ((_, msg) in pending) {
                val success = chatPeer.sendMessage(
                    peer.address.hostAddress,
                    peer.tcpPort,
                    ChatMessage(
                        msg.messageId,
                        msg.senderId,
                        msg.senderName,
                        msg.content
                    )
                )
                if (success) {
                    DatabaseManager.markAsSent(msg.messageId)
                    // Update UI for the resent message if it's the current peer
                    if (peer.id == selectedPeer?.id) {
                        onMessageReceived?.invoke(msg.copy(isPending = false))
                    }
                } else {
                    // Stop if we hit a failure, maybe peer went offline again
                    break
                }
            }
        }
    }

    private fun handleIncomingMessage(incomingMsg: ChatMessage) {
        val timestamp = System.currentTimeMillis()
        scope.launch {
            DatabaseManager.saveMessage(
                peerId = incomingMsg.senderId,
                senderId = incomingMsg.senderId,
                senderName = incomingMsg.sender,
                content = incomingMsg.content,
                timestamp = timestamp,
                messageId = incomingMsg.id,
                isPending = false
            )
        }

        val msg = ChatHistoryMessage(
            messageId = incomingMsg.id,
            senderId = incomingMsg.senderId,
            senderName = incomingMsg.sender,
            content = incomingMsg.content,
            timeSend = timestamp
        )

        if (incomingMsg.senderId == selectedPeer?.id) {
            onMessageReceived?.invoke(msg)
        } else {
            peers.find { it.id == incomingMsg.senderId }?.let { peer ->
                peer.hasUnread = true
                onPeerListRefresh?.invoke()
            }
        }
    }

    private fun handleDiscoveredPeer(peer: PeerInfo) {
        Platform.runLater {
            val existing = peers.find { it.id == peer.id }
            if (existing == null) {
                logger.info("Discovered new peer: {}, {}, {}:{}", peer.id, peer.name, peer.address, peer.tcpPort)
                peers.add(peer)
                scope.launch {
                    DatabaseManager.savePeer(peer.id, peer.name, peer.lastSeen)
                }
            } else {
                val wasOffline = !existing.isOnline
                existing.lastSeen = peer.lastSeen
                existing.isOnline = true
                existing.tcpPort = peer.tcpPort
                existing.address = peer.address
                existing.name = peer.name
                onPeerListRefresh?.invoke()
                scope.launch {
                    DatabaseManager.savePeer(existing.id, existing.name, existing.lastSeen)
                }
                if (wasOffline) {
                    resendPendingMessages(existing)
                }
            }
        }
    }

    private fun startOnlineStatusChecker() {
        scope.launch {
            while (isActive) {
                delay(1000.milliseconds)
                val now = System.currentTimeMillis()
                var needsRefresh = false

                peers.forEach {
                    val wasOnline = it.isOnline
                    it.isOnline = (now - it.lastSeen) < 5000
                    if (wasOnline != it.isOnline) {
                        needsRefresh = true
                        if (!wasOnline && it.isOnline) {
                            resendPendingMessages(it)
                        }
                    }
                }

                if (needsRefresh) {
                    onPeerListRefresh?.invoke()
                }
            }
        }
    }

    private fun formatHistory(history: List<ChatHistoryMessage>): String {
        return history.joinToString("\n") { "${it.senderName}: ${it.content}" }
    }

    fun getMyId(): String = discovery.instanceId

    fun stop() {
        scope.cancel()
    }
}
