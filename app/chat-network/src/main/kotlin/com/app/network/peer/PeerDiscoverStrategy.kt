package com.app.network.peer

import com.app.common.PeerInfo
import java.lang.AutoCloseable

interface PeerDiscoveryStrategy : AutoCloseable {
    val instanceId: String
    fun startScanning(onPeerFound: (PeerInfo) -> Unit)
    fun startAdvertising(userName: String, tcpPort: Int)
    fun stop()
    
    override fun close() {
        stop()
    }
}
