package com.app.ui

import com.app.common.config.ConfigManager
import com.app.network.peer.LocalPeerDiscoveryStrategy
import com.app.network.peer.PeerDiscoveryStrategy
import javafx.application.Application
import javafx.scene.Scene
import javafx.stage.Stage
import org.stefan.chat.ChatPeer
import kotlin.system.exitProcess

class MainChatWindow : Application() {

    private val chatPeer = ChatPeer()
    private val discoveryStrategy: PeerDiscoveryStrategy = LocalPeerDiscoveryStrategy()
    private lateinit var controller: MainController

    override fun start(primaryStage: Stage) {
        ConfigManager.load()
        println(ConfigManager.current)

        controller = MainController(chatPeer, discoveryStrategy)
        val mainView = MainView(controller)

        controller.start()

        primaryStage.scene = Scene(mainView, 800.0, 600.0)
        primaryStage.title = "Kotlin P2P Chat"
        primaryStage.show()
    }

    override fun stop() {
        controller.stop()
        discoveryStrategy.close()
        super.stop()
        exitProcess(0)
    }
}

fun main(args: Array<String>) {
    Application.launch(MainChatWindow::class.java, *args)
}
