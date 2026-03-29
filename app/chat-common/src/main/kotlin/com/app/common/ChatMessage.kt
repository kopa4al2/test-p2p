package com.app.common

import kotlinx.serialization.Serializable

@Serializable
data class ChatMessage(
    val id: String,
    val senderId: String,
    val sender: String,
    val content: String,
    val timestamp: Long = System.currentTimeMillis()
)
