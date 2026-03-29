package org.stefan.chat

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import com.app.common.ChatMessage

class ChatPeer {
    val tcpPort = CompletableDeferred<Int>()

    fun sendMessage(targetIp: String, targetPort: Int, message: ChatMessage) =
        CoroutineScope(Dispatchers.IO).launch {
            try {
                java.net.Socket(targetIp, targetPort).use { socket ->
                    val json = Json.encodeToString(message)
                    socket.getOutputStream().bufferedWriter().use { writer ->
                        writer.write(json)
                        writer.newLine()
                        writer.flush()
                    }
                }
            } catch (e: Exception) {
                println("Грешка: ${e.message}")
            }
        }


    fun startServer(onReceived: (ChatMessage) -> Unit) =
        CoroutineScope(Dispatchers.IO).launch {
            val server = java.net.ServerSocket(0)
            tcpPort.complete(server.localPort)

            while (isActive) {
                server.accept().use { client ->
                    val json = client.getInputStream().bufferedReader().readLine()
                    val msg = Json.decodeFromString<ChatMessage>(json)
                    println("Received message: $msg")
                    onReceived(msg)
                }
            }
        }
}
