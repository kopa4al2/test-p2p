package com.app.ui.components

import com.app.common.PeerInfo
import javafx.beans.property.ReadOnlyObjectProperty
import javafx.collections.ObservableList
import javafx.scene.control.Label
import javafx.scene.control.ListCell
import javafx.scene.control.ListView
import javafx.scene.layout.HBox
import javafx.scene.layout.Priority
import javafx.scene.layout.VBox

class PeerListView(peers: ObservableList<PeerInfo>) : VBox() {
    private val listView = ListView(peers)
    val selectedPeer: PeerInfo?
        get() = listView.selectionModel.selectedItem

    val selectedPeerProperty: ReadOnlyObjectProperty<PeerInfo>
        get() = listView.selectionModel.selectedItemProperty()

    init {
        children.addAll(Label("Online Peers"), listView)
        VBox.setVgrow(listView, Priority.ALWAYS)
        setupCellFactory()
    }

    private fun setupCellFactory() {
        listView.setCellFactory {
            object : ListCell<PeerInfo>() {
                private val statusDot = javafx.scene.shape.Circle(5.0)
                private val content = HBox(statusDot, Label()).apply {
                    spacing = 8.0
                    alignment = javafx.geometry.Pos.CENTER_LEFT
                }

                override fun updateItem(peer: PeerInfo?, empty: Boolean) {
                    super.updateItem(peer, empty)
                    if (empty || peer == null) {
                        graphic = null
                    } else {
                        val label = content.children[1] as Label
                        val addressText = if (peer.address.hostAddress == "0.0.0.0") "offline" else peer.address.hostAddress
                        label.text = "${peer.name} ($addressText)"

                        // Сменяме цвета според статуса
                        statusDot.fill = if (peer.isOnline)
                            javafx.scene.paint.Color.GREEN
                        else
                            javafx.scene.paint.Color.GRAY

                        graphic = content
                    }
                }
            }
        }
    }

    fun onPeerSelected(handler: (PeerInfo?) -> Unit) {
        listView.selectionModel.selectedItemProperty().addListener { _, _, peer -> handler(peer) }
    }

    fun refresh() {
        listView.refresh()
    }

}
