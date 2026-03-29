package org.stefan.chat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.InetSocketAddress
import java.util.*

class PeerDiscovery(private val port: Int = 8889,
                    private val broadcastAddress: InetAddress = InetAddress.getByName("255.255.255.255")) {


    val instanceId = UUID.randomUUID().toString()
    private val broadcastDelayMillis = 1000L

    private val stunServer = InetSocketAddress("YOUR_STUN_IP", 5000)

    // Канал 1: Локален Broadcast
    fun startLocalDiscovery() { /* твоят стар код */ }

    // Канал 2: STUN Discovery
    fun startInternetDiscovery(userName: String) = CoroutineScope(Dispatchers.IO).launch {
        val socket = DatagramSocket(port)
        socket.reuseAddress = true

        // 1. Регистрираме се в STUN сървъра
        val regMsg = "REGISTER:$userName".toByteArray()
        socket.send(DatagramPacket(regMsg, regMsg.size, stunServer))

        // 2. Слушаме за отговори (PEER_INFO)
        while (isActive) {
            val packet = DatagramPacket(ByteArray(1024), 1024)
            socket.receive(packet)
            val data = String(packet.data, 0, packet.length)

            if (data.startsWith("PEER_INFO:")) {
                // Магията: Веднага правим Hole Punch към новия пиър
                handlePeerInfo(data, socket)
            }
        }
    }

    private fun handlePeerInfo(data: String, socket: DatagramSocket) {
        val parts = data.split(":") // PEER_INFO:Name:IP:Port
        val peerIp = InetAddress.getByName(parts[2])
        val peerPort = parts[3].toInt()

        // ПРАВИМ ХОУЛ ПЪНЧ: Пращаме празен пакет, за да отворим рутера
        val punch = "HOLE_PUNCH".toByteArray()
        socket.send(DatagramPacket(punch, punch.size, peerIp, peerPort))

        // Сега вече можем да го добавим в UI списъка
        // onPeerFound(...)
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

    fun startListening(onPeerFound: (id: String, username: String, address: InetAddress, port: Int) -> Unit) = CoroutineScope(Dispatchers.IO).launch {
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
