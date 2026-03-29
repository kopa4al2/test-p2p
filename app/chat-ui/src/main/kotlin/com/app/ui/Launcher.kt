package com.app.ui

import javafx.application.Application

class Launcher {
    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            Application.launch(MainChatWindow::class.java, *args)
        }
    }
}
