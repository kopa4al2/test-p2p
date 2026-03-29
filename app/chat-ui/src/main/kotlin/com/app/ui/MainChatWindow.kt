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
    private lateinit var controller: MainController
    private var discoveryStrategy: PeerDiscoveryStrategy? = null

    override fun start(primaryStage: Stage) {
        ConfigManager.load()
        println(ConfigManager.current)

        val strategy = LocalPeerDiscoveryStrategy(
            instanceId = ConfigManager.current.userId
        )
        this.discoveryStrategy = strategy

        controller = MainController(chatPeer, strategy)
        val mainView = MainView(controller)

        controller.start()

        primaryStage.scene = Scene(mainView, 800.0, 600.0)
        primaryStage.title = "Kotlin P2P Chat - ${ConfigManager.current.userName}"
        primaryStage.show()
    }

    override fun stop() {
        if (::controller.isInitialized) {
            controller.stop()
        }
        discoveryStrategy?.close()
        super.stop()
        exitProcess(0)
    }
}

fun main(args: Array<String>) {
    Application.launch(MainChatWindow::class.java, *args)
}
