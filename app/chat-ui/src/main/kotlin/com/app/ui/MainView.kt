package com.app.ui

import com.app.ui.components.ChatAreaView
import com.app.ui.components.PeerListView
import javafx.application.Platform
import javafx.scene.layout.BorderPane

class MainView(private val controller: MainController) : BorderPane() {
    private val peerListComp = PeerListView(controller.peers)
    private val chatAreaComp = ChatAreaView { text -> controller.sendMessage(text) }

    init {
        left = peerListComp
        center = chatAreaComp

        peerListComp.onPeerSelected { peer ->
            controller.selectPeer(peer)
        }

        chatAreaComp.bindDisableState(peerListComp.selectedPeerProperty.isNull)

        controller.onMessageReceived = { sender, text ->
            Platform.runLater {
                chatAreaComp.appendMessage("$sender: $text")
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
