package com.app.ui.components

import com.app.common.ChatHistoryMessage
import javafx.beans.binding.BooleanExpression
import javafx.collections.FXCollections
import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.control.*
import javafx.scene.layout.HBox
import javafx.scene.layout.Priority
import javafx.scene.layout.VBox
import org.kordamp.ikonli.javafx.FontIcon
import org.kordamp.ikonli.materialdesign.MaterialDesign
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class ChatAreaView(val onSend: (String) -> Unit) : VBox() {
    private val messages = FXCollections.observableArrayList<ChatHistoryMessage>()
    private val listView = ListView(messages)
    private val input = TextField()
    private val sendBtn = Button("Send", FontIcon.of(MaterialDesign.MDI_SEND, 16)).apply {
        style = "-fx-background-color: #075E54; -fx-text-fill: white; -fx-background-radius: 5;"
    }

    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
        .withZone(ZoneId.systemDefault())

    init {
        setupListView()
        input.promptText = "Type a message..."
        input.style = "-fx-background-radius: 5; -fx-padding: 8;"
        HBox.setHgrow(input, Priority.ALWAYS)

        val inputLine = HBox(input, sendBtn).apply { 
            spacing = 10.0
            padding = Insets(10.0)
            style = "-fx-background-color: #F0F0F0;"
        }
        
        val header = Label("Conversation").apply {
            style = "-fx-font-size: 1.2em; -fx-font-weight: bold; -fx-padding: 10;"
        }

        children.addAll(header, listView, inputLine)
        VBox.setVgrow(listView, Priority.ALWAYS)

        sendBtn.setOnAction { 
            if (input.text.isNotBlank()) {
                onSend(input.text)
                input.clear()
            }
        }
        
        input.setOnAction { sendBtn.fire() }
    }

    private fun setupListView() {
        listView.setCellFactory {
            object : ListCell<ChatHistoryMessage>() {
                private val bubble = Label().apply {
                    isWrapText = true
                    maxWidth = 300.0
                    padding = Insets(8.0)
                }
                private val timeLabel = Label().apply {
                    style = "-fx-font-size: 0.8em; -fx-text-fill: gray;"
                }
                private val container = VBox(bubble, timeLabel).apply {
                    spacing = 2.0
                }
                private val root = HBox(container).apply {
                    padding = Insets(5.0)
                }

                override fun updateItem(item: ChatHistoryMessage?, empty: Boolean) {
                    super.updateItem(item, empty)
                    if (empty || item == null) {
                        graphic = null
                    } else {
                        bubble.text = item.content
                        timeLabel.text = timeFormatter.format(Instant.ofEpochMilli(item.timeSend))
                        
                        val isMine = item.senderName == "Me"
                        val isSystem = item.senderName == "System"
                        
                        if (isSystem) {
                            root.alignment = Pos.CENTER
                            container.alignment = Pos.CENTER
                            bubble.style = "-fx-background-color: #EEEEEE; -fx-background-radius: 5; -fx-text-fill: #666666; -fx-font-style: italic;"
                            bubble.maxWidth = 500.0
                        } else if (isMine) {
                            root.alignment = Pos.CENTER_RIGHT
                            container.alignment = Pos.TOP_RIGHT
                            bubble.style = "-fx-background-color: #DCF8C6; -fx-background-radius: 10; -fx-text-fill: black;"
                        } else {
                            root.alignment = Pos.CENTER_LEFT
                            container.alignment = Pos.TOP_LEFT
                            bubble.style = "-fx-background-color: #FFFFFF; -fx-background-radius: 10; -fx-border-color: #E0E0E0; -fx-border-radius: 10; -fx-text-fill: black;"
                        }
                        graphic = root
                    }
                }
            }
        }
    }

    fun updateHistory(historyList: List<ChatHistoryMessage>) {
        messages.setAll(historyList)
        listView.scrollTo(messages.size - 1)
    }
    
    fun appendMessage(msg: ChatHistoryMessage) {
        messages.add(msg)
        listView.scrollTo(messages.size - 1)
    }

    fun bindDisableState(property: BooleanExpression) {
        sendBtn.disableProperty().bind(property)
        input.disableProperty().bind(property)
    }
}
