package com.app.common

data class ChatHistoryMessage(
    val messageId: String,
    val senderId: String,
    val senderName: String,
    val content: String,
    val timeSend: Long = System.currentTimeMillis(),
    val timeSeen: Long = -1
)
