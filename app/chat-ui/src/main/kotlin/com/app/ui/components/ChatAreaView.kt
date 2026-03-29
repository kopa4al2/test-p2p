package com.app.ui.components

import javafx.beans.binding.BooleanExpression
import javafx.scene.control.Button
import javafx.scene.control.Label
import javafx.scene.control.TextArea
import javafx.scene.control.TextField
import javafx.scene.layout.HBox
import javafx.scene.layout.Priority
import javafx.scene.layout.VBox

class ChatAreaView(val onSend: (String) -> Unit) : VBox() {
    private val history = TextArea().apply { isEditable = false }
    private val input = TextField()
    private val sendBtn = Button("Send")

    init {
        val inputLine = HBox(input, sendBtn).apply { spacing = 10.0 }
        children.addAll(Label("Conversation"), history, inputLine)
        VBox.setVgrow(history, Priority.ALWAYS)

        sendBtn.setOnAction { 
            if (input.text.isNotBlank()) {
                onSend(input.text)
                input.clear()
            }
        }
    }

    fun updateHistory(text: String) {
        history.text = text + "\n"
        history.scrollTop = Double.MAX_VALUE
    }
    
    fun appendMessage(line: String) {
        history.appendText(line + "\n")
    }

    fun bindDisableState(property: BooleanExpression) {
        sendBtn.disableProperty().bind(property)
    }
}
