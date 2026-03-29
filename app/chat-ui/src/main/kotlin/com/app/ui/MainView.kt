package com.app.ui

import com.app.ui.components.ChatAreaView
import com.app.ui.components.PeerListView
import javafx.application.Platform
import javafx.scene.layout.BorderPane
import org.controlsfx.control.StatusBar

class MainView(private val controller: MainController) : BorderPane() {
    private val peerListComp = PeerListView(controller.peers)
    private val chatAreaComp = ChatAreaView(controller.getMyId()) { text -> controller.sendMessage(text) }
    private val statusBar = StatusBar().apply {
        text = "Ready"
        style = "-fx-background-color: #EEEEEE;"
    }

    init {
        left = peerListComp
        center = chatAreaComp
        bottom = statusBar

        peerListComp.onPeerSelected { peer ->
            controller.selectPeer(peer)
            statusBar.text = if (peer != null) "Chatting with ${peer.name}" else "Ready"
        }

        chatAreaComp.bindDisableState(peerListComp.selectedPeerProperty.isNull)

        controller.onMessageReceived = { msg ->
            Platform.runLater {
                chatAreaComp.appendMessage(msg)
            }
        }

        controller.onHistoryUpdated = { history ->
            Platform.runLater {
                chatAreaComp.updateHistory(history)
            }
        }

        controller.onPeerListRefresh = {
            Platform.runLater {
                peerListComp.refresh()
            }
        }
    }
}
