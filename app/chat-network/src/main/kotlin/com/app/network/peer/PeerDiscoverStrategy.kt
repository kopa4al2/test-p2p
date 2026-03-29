package com.app.network.peer

import com.app.common.PeerInfo

interface PeerDiscoveryStrategy {
    fun startDiscovery(userName: String, onPeerFound: (PeerInfo) -> Unit)
    fun stopDiscovery()
}
