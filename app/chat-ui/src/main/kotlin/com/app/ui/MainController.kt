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
import org.stefan.chat.ChatPeer
import java.net.InetAddress
import java.util.*
import kotlin.time.Duration.Companion.milliseconds

class MainController(
    private val chatPeer: ChatPeer,
    private val discovery: PeerDiscoveryStrategy
) {
    val peers: ObservableList<PeerInfo> = FXCollections.observableArrayList()
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val dispatcherIO = Dispatchers.IO

    var onMessageReceived: ((String, String) -> Unit)? = null
    var onHistoryUpdated: ((String) -> Unit)? = null
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
            println("Server started on port: $port")
            discovery.startAdvertising(ConfigManager.current.userName, port)

            discovery.startScanning { peer ->
                handleDiscoveredPeer(peer)
            }
        }
        startOnlineStatusChecker()
    }

    private fun loadKnownPeers() {
        scope.launch(dispatcherIO) {
            val knownPeers = DatabaseManager.getAllPeers()
            withContext(Dispatchers.Main) {
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
    }

    fun selectPeer(peer: PeerInfo?) {
        selectedPeer = peer
        if (peer != null) {
            scope.launch {
                val history = withContext(dispatcherIO) {
                    DatabaseManager.getHistory(peer.id)
                }
                onHistoryUpdated?.invoke(formatHistory(history))
            }
        } else {
            onHistoryUpdated?.invoke("")
        }
    }

    fun sendMessage(text: String) {
        val peer = selectedPeer ?: return
        if (!peer.isOnline) {
            onMessageReceived?.invoke("System", "Peer is currently offline. Message will be sent when they come online.")
        }
        val myId = discovery.instanceId
        val messageId = UUID.randomUUID().toString()
        val timestamp = System.currentTimeMillis()

        scope.launch {
            withContext(dispatcherIO) {
                DatabaseManager.saveMessage(
                    peerId = peer.id,
                    senderId = myId,
                    senderName = "Me",
                    content = text,
                    timestamp = timestamp,
                    messageId = messageId,
                    isPending = true
                )
            }

            if (peer.isOnline) {
                val success = chatPeer.sendMessage(
                    peer.address.hostAddress,
                    peer.tcpPort,
                    ChatMessage(
                        messageId,
                        myId,
                        ConfigManager.current.userName,
                        text
                    )
                )
                if (success) {
                    withContext(dispatcherIO) {
                        DatabaseManager.markAsSent(messageId)
                    }
                }
            }
        }

        onMessageReceived?.invoke("Me", text)
    }

    private fun resendPendingMessages(peer: PeerInfo) {
        if (!peer.isOnline) return
        scope.launch(dispatcherIO) {
            val pending = DatabaseManager.getPendingMessages().filter { it.first == peer.id }
            if (pending.isEmpty()) return@launch

            println("Resending ${pending.size} pending messages to ${peer.name}")
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
                } else {
                    // Stop if we hit a failure, maybe peer went offline again
                    break
                }
            }
        }
    }

    private fun handleIncomingMessage(incomingMsg: ChatMessage) {
        scope.launch(dispatcherIO) {
            DatabaseManager.saveMessage(
                peerId = incomingMsg.senderId,
                senderId = incomingMsg.senderId,
                senderName = incomingMsg.sender,
                content = incomingMsg.content,
                timestamp = System.currentTimeMillis(),
                messageId = incomingMsg.id,
                isPending = false
            )
        }

        if (incomingMsg.senderId == selectedPeer?.id) {
            onMessageReceived?.invoke(incomingMsg.sender, incomingMsg.content)
        }
    }

    private fun handleDiscoveredPeer(peer: PeerInfo) {
        Platform.runLater {
            val existing = peers.find { it.id == peer.id }
            if (existing == null) {
                println("Discovered new peer: ${peer.id}, ${peer.name}, ${peer.address}:${peer.tcpPort}")
                peers.add(peer)
                scope.launch(dispatcherIO) {
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
                scope.launch(dispatcherIO) {
                    DatabaseManager.savePeer(existing.id, existing.name, existing.lastSeen)
                }
                if (wasOffline) {
                    resendPendingMessages(existing)
                }
            }
        }
    }

    private fun startOnlineStatusChecker() {
        scope.launch(dispatcherIO) {
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

    fun stop() {
        scope.cancel()
    }
}
