package org.stefan.chat

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import com.app.common.ChatMessage
import org.slf4j.LoggerFactory

class ChatPeer {
    private val logger = LoggerFactory.getLogger(ChatPeer::class.java)
    val tcpPort = CompletableDeferred<Int>()

    suspend fun sendMessage(targetIp: String, targetPort: Int, message: ChatMessage): Boolean =
        withContext(Dispatchers.IO) {
            try {
                java.net.Socket().use { socket ->
                    socket.connect(java.net.InetSocketAddress(targetIp, targetPort), 2000)
                    val json = Json.encodeToString(message)
                    socket.getOutputStream().bufferedWriter().use { writer ->
                        writer.write(json)
                        writer.newLine()
                        writer.flush()
                    }
                }
                true
            } catch (e: Exception) {
                logger.error("Send message error to {}:{}: {}", targetIp, targetPort, e.message)
                false
            }
        }


    fun startServer(onReceived: (ChatMessage) -> Unit) =
        CoroutineScope(Dispatchers.IO).launch {
            val server = java.net.ServerSocket(0)
            tcpPort.complete(server.localPort)

            while (isActive) {
                server.accept().use { client ->
                    val json = client.getInputStream().bufferedReader().readLine()
                    logger.debug("Received raw: {}", json)
                    val msg = Json.decodeFromString<ChatMessage>(json)
                    logger.info("Received message from: {}", msg.sender)
                    onReceived(msg)
                }
            }
        }
}
