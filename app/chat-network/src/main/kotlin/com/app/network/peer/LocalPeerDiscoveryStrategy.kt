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
import java.net.Inet4Address
import java.net.NetworkInterface
import java.util.*
import kotlin.time.Duration.Companion.milliseconds

class LocalPeerDiscoveryStrategy(
    override val instanceId: String = UUID.randomUUID().toString(),
    port: Int = 8889
) : PeerDiscoveryStrategy {

    private val broadcastDelayMillis = 1000L
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var broadcastSocket: DatagramSocket? = null
    private var listenSocket: DatagramSocket? = null

    private var userName: String? = null
    private var tcpServerPort: Int? = null
    private val safePort: Int = port.takeIf { it in 1..65535 } ?: 8889

    private val broadcastAddressOverride = InetAddress.getByName("255.255.255.255")

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
                val broadcastAddresses = getBroadcastAddresses()
                if (broadcastAddresses.isEmpty()) {
                    // Fallback to global broadcast
                    val packet = DatagramPacket(message, message.size, broadcastAddressOverride, safePort)
                    socket.send(packet)
                } else {
                    for (address in broadcastAddresses) {
                        val packet = DatagramPacket(message, message.size, address, safePort)
                        socket.send(packet)
                    }
                }
                delay(broadcastDelayMillis.milliseconds)
            }
        } finally {
            socket.close()
        }
    }

    private fun getBroadcastAddresses(): List<InetAddress> {
        val broadcastAddresses = mutableListOf<InetAddress>()
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val networkInterface = interfaces.nextElement()
                if (networkInterface.isLoopback || !networkInterface.isUp) continue

                for (interfaceAddress in networkInterface.interfaceAddresses) {
                    val broadcast = interfaceAddress.broadcast
                    if (broadcast != null && broadcast is Inet4Address) {
                        broadcastAddresses.add(broadcast)
                    }
                }
            }
        } catch (e: Exception) {
            println("Error while getting broadcast addresses: ${e.message}")
        }
        return broadcastAddresses
    }

    private fun startListening(onPeerFound: (id: String, username: String, address: InetAddress, peerPort: Int) -> Unit) =
        scope.launch {
            val socket = DatagramSocket(null).apply {
                reuseAddress = true
                bind(InetSocketAddress(safePort))
            }
            listenSocket = socket

            val buffer = ByteArray(1024)
            println("Listen for peers on port: $safePort...")

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
                if (isActive) println("Error receiving message on port $safePort: ${e.message}")
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
