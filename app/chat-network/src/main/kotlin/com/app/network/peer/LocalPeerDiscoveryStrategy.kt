package com.app.network.peer

import com.app.common.PeerInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.InetSocketAddress
import java.util.*
import kotlin.time.Duration.Companion.milliseconds

class LocalPeerDiscoveryStrategy(
    private val port: Int = 8889,
    private val broadcastAddress: InetAddress = InetAddress.getByName("255.255.255.255")
) : PeerDiscoveryStrategy {

    override val instanceId = UUID.randomUUID().toString()
    private val broadcastDelayMillis = 1000L
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var broadcastSocket: DatagramSocket? = null
    private var listenSocket: DatagramSocket? = null

    private var userName: String? = null
    private var tcpServerPort: Int? = null
    private val safePort: Int = port.takeIf { it in 1..65535 } ?: 8889

    init {
        if (port !in 1..65535) {
            println("Invalid discovery port '$port'. Falling back to default port 8889.")
        }
    }

    override fun startScanning(onPeerFound: (PeerInfo) -> Unit) {
        startListening(
            onPeerFound = { id, name, address, port ->
                onPeerFound(PeerInfo(id, name, address, port))
            })
    }

    override fun startAdvertising(userName: String, tcpPort: Int) {
        this.userName = userName
        this.tcpServerPort = tcpPort
        broadcastPresence()
    }

    private fun broadcastPresence() = scope.launch {
        val name = userName ?: return@launch
        val tcpPort = tcpServerPort ?: return@launch

        val socket = DatagramSocket().apply { broadcast = true }
        broadcastSocket = socket
        val message = "$instanceId|$name|$tcpPort".toByteArray()

        try {
            while (isActive) {
                val packet = DatagramPacket(
                    message, message.size,
                    broadcastAddress, port
                )
                socket.send(packet)
                delay(broadcastDelayMillis.milliseconds)
            }
        } finally {
            socket.close()
        }
    }

    private fun startListening(onPeerFound: (id: String, username: String, address: InetAddress, peerPort: Int) -> Unit) =
        scope.launch {
            val socket = DatagramSocket(null).apply {
                reuseAddress = true
                bind(InetSocketAddress(safePort))
            }
            listenSocket = socket

            val buffer = ByteArray(1024)
            println("Listen for peers on port: $port...")

            try {
                while (isActive) {
                    val packet = DatagramPacket(buffer, buffer.size)
                    socket.receive(packet)
                    val data = String(packet.data, 0, packet.length)
                    val parts = data.split("|")

                    if (parts.size == 3) {
                        val id = parts[0]
                        val name = parts[1]
                        val tcpPort = parts[2].toIntOrNull() ?: continue

                        if (id != instanceId) {
                            onPeerFound(id, name, packet.address, tcpPort)
                        }
                    }
                }
            } catch (e: Exception) {
                if (isActive) println("Error receiving message: ${e.message}")
            } finally {
                socket.close()
            }
        }

    override fun stop() {
        scope.cancel()
        broadcastSocket?.close()
        listenSocket?.close()
    }
}
