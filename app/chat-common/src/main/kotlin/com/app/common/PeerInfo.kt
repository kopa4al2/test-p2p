package com.app.common

import java.net.InetAddress

data class PeerInfo(
    val id: String,
    val name: String,
    val address: InetAddress,
    val tcpPort: Int,
    var lastSeen: Long = System.currentTimeMillis(),
    var isOnline: Boolean = true
)
