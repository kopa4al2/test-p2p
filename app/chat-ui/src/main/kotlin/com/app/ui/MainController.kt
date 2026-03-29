package com.app.ui

import com.app.common.ChatHistoryMessage
import com.app.common.ChatMessage
import com.app.common.PeerInfo
import com.app.common.config.ConfigManager
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
    private val messageHistories = mutableMapOf<String, MutableList<ChatHistoryMessage>>()
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    var onMessageReceived: ((String, String) -> Unit)? = null
    var onHistoryUpdated: ((String) -> Unit)? = null
    var onPeerListRefresh: (() -> Unit)? = null
    var selectedPeer: PeerInfo? = null

    fun start() {
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

    fun selectPeer(peer: PeerInfo?) {
        selectedPeer = peer
        if (peer != null) {
            val history = messageHistories.getOrPut(peer.id) { mutableListOf() }
            onHistoryUpdated?.invoke(formatHistory(history))
        } else {
            onHistoryUpdated?.invoke("")
        }
    }

    fun sendMessage(text: String) {
        val peer = selectedPeer ?: return
        val myId = discovery.instanceId
        chatPeer.sendMessage(
            peer.address.hostAddress,
            peer.tcpPort,
            ChatMessage(
                UUID.randomUUID().toString(),
                myId,
                ConfigManager.current.userName,
                text
            )
        )

        val history = messageHistories.getOrPut(peer.id) { mutableListOf() }
        history.add(ChatHistoryMessage(myId, "Me", text, System.currentTimeMillis()))
        onMessageReceived?.invoke("Me", text)
    }

    private fun handleIncomingMessage(incomingMsg: ChatMessage) {
        val history = messageHistories.getOrPut(incomingMsg.senderId) { mutableListOf() }
        history.add(
            ChatHistoryMessage(
                incomingMsg.senderId,
                incomingMsg.sender,
                incomingMsg.content,
                System.currentTimeMillis()
            )
        )

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
            } else {
                existing.lastSeen = System.currentTimeMillis()
                existing.isOnline = true
                onPeerListRefresh?.invoke()
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
                    if (wasOnline != it.isOnline) needsRefresh = true
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
