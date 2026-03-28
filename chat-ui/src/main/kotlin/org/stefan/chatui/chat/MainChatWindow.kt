package org.stefan.chatui.chat

import PeerInfo
import javafx.application.Application
import javafx.application.Platform
import javafx.collections.FXCollections
import javafx.scene.Scene
import javafx.scene.control.Button
import javafx.scene.control.Label
import javafx.scene.control.ListCell
import javafx.scene.control.ListView
import javafx.scene.control.SplitPane
import javafx.scene.control.TextArea
import javafx.scene.control.TextField
import javafx.scene.layout.HBox
import javafx.scene.layout.Priority
import javafx.scene.layout.VBox
import javafx.stage.Stage
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import org.stefan.ChatMessage
import org.stefan.chat.ChatPeer
import org.stefan.chat.PeerDiscovery
import java.net.InetAddress

class MainChatWindow : Application() {
    private val peers = FXCollections.observableArrayList<PeerInfo>()
    private val discoveredPeers = mutableMapOf<String, PeerInfo>()

    private val chatHistory = TextArea().apply { isEditable = false }
    private val messageInput = TextField()
    private val peerList = ListView(peers)

    private val chatPeer = ChatPeer()
    private val peerDiscovery = PeerDiscovery()

    override fun start(primaryStage: Stage) {
        peerList.setCellFactory {
            object : ListCell<PeerInfo>() {
                override fun updateItem(peer: PeerInfo?, empty: Boolean) {
                    super.updateItem(peer, empty)
                    if (empty || peer == null) {
                        text = null
                    } else {
                        text = "${peer.name} (${peer.address.hostAddress})"
                    }
                }
            }
        }

        MainScope().launch {
            chatPeer.startServer(appendChatMessage())

            val port = chatPeer.tcpPort.await()

            peerDiscovery.startBroadcasting("MyUser", port)

            println("Server started on port: $port")

            peerDiscovery.startListening { id, name, address, remoteTcpPort ->
                Platform.runLater {
                    updatePeerList(id, name, address, remoteTcpPort)
                }
            }
        }


        val sidebar = VBox(Label("Online Peers:"), peerList).apply {
            spacing = 10.0
            style = "-fx-background-color: #f0f0f0; -fx-padding: 10;"
        }

        // Дясна част: Чат история и вход
        val sendBtn = Button("Send")
        val inputArea = HBox(messageInput, sendBtn).apply { spacing = 10.0 }
        val mainChat = VBox(Label("Chat:"), chatHistory, inputArea).apply {
            spacing = 10.0
            VBox.setVgrow(chatHistory, Priority.ALWAYS)
        }

        val root = SplitPane(sidebar, mainChat).apply {
            setDividerPositions(0.3)
        }

        // Логика за изпращане
        sendBtn.setOnAction {
            val selectedPeer = peerList.selectionModel.selectedItem
            println("Selected peer: $selectedPeer")
            val text = messageInput.text
            if (selectedPeer != null && text.isNotBlank()) {
                chatPeer.sendMessage(selectedPeer.address.hostAddress, selectedPeer.tcpPort, ChatMessage(peerDiscovery.instanceId, text));
                chatHistory.appendText("Me: $text\n")
                messageInput.clear()
            }
        }



        primaryStage.scene = Scene(root, 1980.0, 1280.0)
        primaryStage.title = "Kotlin P2P Chat"
        primaryStage.show()
    }

    private fun appendChatMessage() = { chatMessage: ChatMessage ->
        Platform.runLater {
            chatHistory.appendText("${chatMessage.timestamp} ${chatMessage.sender}: ${chatMessage.content}\n")
        }
    }

    private fun updatePeerList(id: String, name: String, address: InetAddress, remoteTcpPort: Int) {
        if (discoveredPeers.containsKey(id)) return

        val peerInfo = PeerInfo(id, name, address, remoteTcpPort)
        discoveredPeers[id] = peerInfo
        peers.add(peerInfo)
    }

    private fun removePeer(peerInfo: PeerInfo) {}

    private fun appendMessage(message: String) {
        chatHistory.appendText(message)
    }

}