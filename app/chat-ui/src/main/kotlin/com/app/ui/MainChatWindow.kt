package com.app.ui

import com.app.common.config.ConfigLoader
import com.app.common.config.ConfigManager
import com.app.network.peer.LocalPeerDiscoveryStrategy
import javafx.application.Application
import javafx.application.Platform
import javafx.scene.Scene
import javafx.stage.Stage
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import org.stefan.chat.ChatPeer
import org.stefan.chat.PeerDiscovery

class MainChatWindow : Application() {

    private val chatPeer = ChatPeer()
    private val discovery = PeerDiscovery()
    private val discoveryStrategy = LocalPeerDiscoveryStrategy()

    override fun start(primaryStage: Stage) {
        ConfigManager.load()
        println(ConfigManager.current)
        val mainView = MainView(chatPeer, discovery)
        MainScope().launch {
            chatPeer.startServer { incomingMsg ->
                Platform.runLater {
                    mainView.handleIncomingMessage(incomingMsg)
                }
            }

            val port = chatPeer.tcpPort.await()

            discovery.startBroadcasting("Stefan", port)

            println("Server started on port: $port")

            discovery.startListening { peerId, peerName, peerAddress, peerTcpPort ->
                mainView.handleDiscoveredPeer(peerId, peerName, peerAddress, peerTcpPort)
            }
        }


        primaryStage.scene = Scene(mainView, 800.0, 600.0)
        primaryStage.title = "Kotlin P2P Chat"
        primaryStage.show()
    }

    override fun stop() {
        // TODO: close sockets
        // chatPeer.stop(), discovery.stop() и т.н.
        super.stop()
    }

}
