package org.stefan.chatui

import javafx.application.Application
import javafx.scene.Scene
import javafx.scene.control.Button
import javafx.scene.control.Label
import javafx.scene.layout.VBox
import javafx.stage.Stage
import org.stefan.chat.PeerDiscovery


class ChatUiApplication : Application() {

    private val discovery: PeerDiscovery = PeerDiscovery()

    override fun start(primaryStage: Stage) {
//        // Create UI components for the main window
//        val label = Label("Welcome to Chat UI!")
//        val button = Button("Click me")
//
//        // 1. Стартираме слушането веднага при пускане на приложението
//        discovery.startListening { name, address ->
//            // ВАЖНО: JavaFX изисква промените по UI да стават в неговата нишка!
//            javafx.application.Platform.runLater {
//                label.text = "Намерен: $name на $address"
//            }
//        }
//
//        button.setOnAction {
//            // 2. При клик изпращаме сигнал "Аз съм тук"
//            // Използваме GlobalScope или собствена нишка, за да не блокираме интерфейса
//            discovery.startBroadcasting("User-${System.currentTimeMillis() % 1000}")
//            label.text = "Сигналът е изпратен!"
//        }
//
//        val layout = VBox(10.0, label, button)
//        layout.style = "-fx-padding: 20; -fx-alignment: center; -fx-spacing: 10;"
//
//        // Set up the scene
//        val scene = Scene(layout, 400.0, 300.0)
//
//        // Set up the stage (main window)
//        primaryStage.title = "Chat UI"
//        primaryStage.scene = scene
//        primaryStage.show()
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            launch(ChatUiApplication::class.java, *args)
        }
    }

}