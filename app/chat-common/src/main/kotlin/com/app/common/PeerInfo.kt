package com.app.common

import java.net.InetAddress

data class PeerInfo(
    val id: String,
    var name: String,
    var address: InetAddress,
    var tcpPort: Int,
    var lastSeen: Long = System.currentTimeMillis(),
    var isOnline: Boolean = true
)
