package com.app.common.db

import com.app.common.ChatHistoryMessage
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.io.File

object DatabaseManager {
    fun init(userId: String = "default") {
        val dbFileName = "p2p_$userId.db"
        val dbFile = File(dbFileName)
        println("Initializing database at: ${dbFile.absolutePath}")
        Database.connect("jdbc:sqlite:${dbFile.absolutePath}", "org.sqlite.JDBC")

        transaction {
//            exec("PRAGMA journal_mode=WAL;")
//            exec("PRAGMA synchronous=NORMAL;")
            SchemaUtils.create(Messages)
            SchemaUtils.create(Peers)
        }
    }

    fun savePeer(peerId: String, peerName: String, lastOnline: Long = System.currentTimeMillis()) {
        transaction {
            val existing = Peers.selectAll().where { Peers.peerId eq peerId }.firstOrNull()
            if (existing == null) {
                Peers.insert {
                    it[Peers.peerId] = peerId
                    it[Peers.peerName] = peerName
                    it[Peers.lastOnline] = lastOnline
                }
            } else {
                Peers.update({ Peers.peerId eq peerId }) {
                    it[Peers.peerName] = peerName
                    it[Peers.lastOnline] = lastOnline
                }
            }
        }
    }

    fun getAllPeers(): List<Triple<String, String, Long>> {
        return transaction {
            Peers.selectAll().map {
                Triple(it[Peers.peerId], it[Peers.peerName], it[Peers.lastOnline])
            }
        }
    }

    fun saveMessage(
        peerId: String,
        senderId: String,
        senderName: String,
        content: String,
        timestamp: Long,
        messageId: String,
        isPending: Boolean = false
    ) {
        transaction {
            val exists = Messages.selectAll().where { Messages.messageId eq messageId }.any()
            if (!exists) {
                Messages.insert {
                    it[Messages.peerId] = peerId
                    it[Messages.senderId] = senderId
                    it[Messages.senderName] = senderName
                    it[Messages.content] = content
                    it[Messages.timestamp] = timestamp
                    it[Messages.messageId] = messageId
                    it[Messages.isPending] = isPending
                }
            }
        }
    }

    fun getHistory(peerId: String): List<ChatHistoryMessage> {
        return transaction {
            Messages.selectAll().where { Messages.peerId eq peerId }
                .orderBy(Messages.timestamp to SortOrder.ASC)
                .map {
                    ChatHistoryMessage(
                        messageId = it[Messages.messageId],
                        senderId = it[Messages.senderId],
                        senderName = it[Messages.senderName],
                        content = it[Messages.content],
                        timeSend = it[Messages.timestamp]
                    )
                }
        }
    }

    fun markAsSent(messageId: String) {
        transaction {
            Messages.update({ Messages.messageId eq messageId }) {
                it[Messages.isPending] = false
            }
        }
    }

    fun getPendingMessages(): List<Pair<String, ChatHistoryMessage>> {
        return transaction {
            Messages.selectAll().where { Messages.isPending eq true }
                .map {
                    val peerId = it[Messages.peerId]
                    val msg = ChatHistoryMessage(
                        messageId = it[Messages.messageId],
                        senderId = it[Messages.senderId],
                        senderName = it[Messages.senderName],
                        content = it[Messages.content],
                        timeSend = it[Messages.timestamp]
                    )
                    peerId to msg
                }
        }
    }
}
