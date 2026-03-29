package com.app.network.peer

import com.app.common.PeerInfo
import kotlinx.coroutines.*
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.InetSocketAddress
import java.util.*

class LocalPeerDiscoveryStrategy(
    private val port: Int = 8889,
    private val broadcastAddress: InetAddress = InetAddress.getByName("255.255.255.255")
) : PeerDiscoveryStrategy {


    val instanceId = UUID.randomUUID().toString()
    private val broadcastDelayMillis = 1000L

    override fun startDiscovery(userName: String, onPeerFound: (PeerInfo) -> Unit) {
        startListening(
            onPeerFound = { id, name, address, port ->
                onPeerFound(PeerInfo(id, name, address, port))
            })
    }

    override fun stopDiscovery() {
        TODO("Not yet implemented")
    }

    fun startBroadcasting(userName: String, tcpServerPort: Int) = CoroutineScope(Dispatchers.IO).launch {
        val socket = DatagramSocket()
        socket.broadcast = true
        val message = "$instanceId|$userName|$tcpServerPort".toByteArray()

        while (isActive) {
            val packet = DatagramPacket(
                message, message.size,
                InetAddress.getByName("255.255.255.255"), port
            )
            socket.send(packet)
            delay(broadcastDelayMillis)
        }
    }

    private fun startListening(onPeerFound: (id: String, username: String, address: InetAddress, port: Int) -> Unit) =
        CoroutineScope(
            Dispatchers.IO
        ).launch {
            val socket = DatagramSocket(null)
            socket.reuseAddress = true
            socket.bind(InetSocketAddress(port))

            val buffer = ByteArray(1024)
            println("Listen for ears on port: $port...")

            while (isActive) {
                try {
                    val packet = DatagramPacket(buffer, buffer.size)
                    socket.receive(packet)
                    val data = String(packet.data, 0, packet.length)
                    val parts = data.split("|")

                    if (parts.size == 3) {
                        val id = parts[0]
                        val name = parts[1]
                        val tcpPort = parts[2].toInt()

                        if (id != instanceId) {
                            onPeerFound(id, name, packet.address, tcpPort)
                        }
                    }
                } catch (e: Exception) {
                    if (isActive) println("Error receive message: ${e.message}")
                }
            }
        }
}
