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
import org.kordamp.ikonli.javafx.FontIcon
import org.kordamp.ikonli.materialdesign.MaterialDesign

class PeerListView(peers: ObservableList<PeerInfo>) : VBox() {
    private val listView = ListView(peers)
    val selectedPeer: PeerInfo?
        get() = listView.selectionModel.selectedItem

    val selectedPeerProperty: ReadOnlyObjectProperty<PeerInfo>
        get() = listView.selectionModel.selectedItemProperty()

    init {
        val header = Label("Peers", FontIcon.of(MaterialDesign.MDI_ACCOUNT_MULTIPLE, 18)).apply {
            style = "-fx-font-size: 1.1em; -fx-font-weight: bold; -fx-padding: 10;"
        }
        style = "-fx-background-color: #F5F5F5; -fx-border-color: #E0E0E0; -fx-border-width: 0 1 0 0;"
        prefWidth = 250.0

        children.addAll(header, listView)
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
                        style = ""
                    } else {
                        val label = content.children[1] as Label
                        val addressText = if (peer.address.hostAddress == "0.0.0.0") "offline" else peer.address.hostAddress
                        label.text = "${peer.name} ($addressText)"

                        // Сменяме цвета според статуса
                        statusDot.fill = if (peer.isOnline)
                            javafx.scene.paint.Color.web("#4CAF50")
                        else
                            javafx.scene.paint.Color.web("#9E9E9E")

                        if (peer.hasUnread) {
                            label.style = "-fx-font-weight: bold; -fx-text-fill: #1976D2;"
                            content.style = "-fx-background-color: #DDDDDD; -fx-background-radius: 5; -fx-padding: 5;"
                        } else {
                            label.style = "-fx-text-fill: #333333;"
                            content.style = "-fx-padding: 5;"
                        }

                        graphic = content
                        
                        // Style the cell itself when selected
                        selectedProperty().addListener { _, _, isSelected ->
                            if (isSelected) {
                                style = "-fx-background-color: #E0E0E0;"
                            } else {
                                style = ""
                            }
                        }
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
