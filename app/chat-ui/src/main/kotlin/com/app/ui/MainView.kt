package com.app.ui

import com.app.common.ChatHistoryMessage
import com.app.common.PeerInfo
import javafx.application.Platform
import javafx.collections.FXCollections
import javafx.scene.layout.BorderPane
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import com.app.common.ChatMessage
import org.stefan.chat.ChatPeer
import org.stefan.chat.PeerDiscovery
import com.app.ui.components.ChatAreaView
import com.app.ui.components.PeerListView
import java.net.InetAddress
import java.util.*

class MainView(val chatPeer: ChatPeer, val discovery: PeerDiscovery) : BorderPane() {
    private val peers = FXCollections.observableArrayList<PeerInfo>()
    private val messageHistories = mutableMapOf<String, MutableList<ChatHistoryMessage>>()

    private val peerListComp = PeerListView(peers)
    private val chatAreaComp = ChatAreaView { text -> sendMessage(text) }

    init {
        left = peerListComp
        center = chatAreaComp

        peerListComp.onPeerSelected { peer ->
            if (peer != null) {
                val history = messageHistories.getOrPut(peer.id) { mutableListOf() }
                chatAreaComp.updateHistory(history.joinToString("\n") { "${it.senderName}: ${it.content}" })
            }
        }

        chatAreaComp.bindDisableState(peerListComp.selectedPeerProperty.isNull)

        startOnlineStatusChecker()
    }

    private fun sendMessage(text: String) {
        val selectedPeer = peerListComp.selectedPeer!!
        chatPeer.sendMessage(
            selectedPeer.address.hostAddress,
            selectedPeer.tcpPort,
            ChatMessage(UUID.randomUUID().toString(), discovery.instanceId, "Stefan", text)
        )
        messageHistories
            .getOrPut(selectedPeer.id) { mutableListOf() }
            .add(ChatHistoryMessage(discovery.instanceId, "Me", text, System.currentTimeMillis()))
        chatAreaComp.appendMessage("\nMe: $text")
    }

    fun handleIncomingMessage(incomingMsg: ChatMessage) {
        messageHistories
            .getOrPut(incomingMsg.senderId) { mutableListOf() }
            .apply {
                add(
                    ChatHistoryMessage(
                        incomingMsg.senderId,
                        incomingMsg.sender,
                        incomingMsg.content,
                        System.currentTimeMillis()
                    )
                )
            }
        println("Received message from ${incomingMsg.sender}: ${incomingMsg.content}. Selected peer: ${peerListComp.selectedPeerProperty.value?.name}")
        if (incomingMsg.senderId == peerListComp.selectedPeer?.id) {
            chatAreaComp.appendMessage("${incomingMsg.sender}: ${incomingMsg.content}")
        }
    }

    private fun startOnlineStatusChecker() {
        MainScope().launch {
            while (isActive) {
                delay(1000)
                val now = System.currentTimeMillis()
                var needsRefresh = false

                peers.forEach {
                    val wasOnline = it.isOnline
                    it.isOnline = (now - it.lastSeen) < 5000
                    if (wasOnline != it.isOnline) needsRefresh = true
                }

                if (needsRefresh) {
                    Platform.runLater { peerListComp.refresh() }
                }
            }
        }
    }

    fun handleDiscoveredPeer(id: String, name: String, address: InetAddress, port: Int) {
        Platform.runLater {
            val existing = peers.find { it.id == id }
            if (existing == null) {
                peers.add(PeerInfo(id, name, address, port))
            } else {
                existing.lastSeen = System.currentTimeMillis()
                existing.isOnline = true

                peerListComp.refresh()
            }
        }
    }
}
