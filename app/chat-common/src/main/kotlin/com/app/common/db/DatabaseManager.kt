package com.app.common.db

import com.app.common.ChatHistoryMessage
import com.app.common.config.ConfigManager
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory
import java.io.File
import java.util.concurrent.Executors

object DatabaseManager {
    private val logger = LoggerFactory.getLogger(DatabaseManager::class.java)
    
    // SQLite only allows one writer at a time. 
    // We use a single-threaded dispatcher to ensure all DB operations are sequential.
    private val dbDispatcher = Executors.newSingleThreadExecutor().asCoroutineDispatcher()
    
    // Additionally use a mutex to ensure that no two coroutines try to write at the same time
    // from within the same single-threaded dispatcher (though redundant, it doesn't hurt).
    private val writeMutex = Mutex()

    fun init(userId: String = "default") {
        val dbFileName = "p2p_$userId.db"
        val dbFile = File(ConfigManager.resourcesFolder(),dbFileName)
        logger.info("Initializing database at: {}", dbFile.absolutePath)
        
        // Set journal_mode and busy_timeout via the connection URL for SQLite
        val url = "jdbc:sqlite:${dbFile.absolutePath}?journal_mode=WAL&busy_timeout=5000"
        Database.connect(url, "org.sqlite.JDBC")

        transaction {
            SchemaUtils.create(Messages)
            SchemaUtils.create(Peers)
        }
    }

    private suspend fun <T> dbQuery(block: () -> T): T = withContext(dbDispatcher) {
        writeMutex.withLock {
            transaction {
                block()
            }
        }
    }

    suspend fun savePeer(peerId: String, peerName: String, lastOnline: Long = System.currentTimeMillis()) = dbQuery {
        val existing = Peers.selectAll().where { Peers.peerId eq peerId }.firstOrNull()
        if (existing == null) {
            Peers.insert {
                it[Peers.peerId] = peerId
                it[Peers.peerName] = peerName
                it[Peers.lastOnline] = lastOnline
            }
        } else {
            // To avoid "database is locked" errors during frequent heartbeats,
            // we only update if the name changed or significant time has passed (e.g., 1 minute).
            val lastSaved = existing[Peers.lastOnline]
            val nameChanged = existing[Peers.peerName] != peerName
            if (nameChanged || (lastOnline - lastSaved > 60_000)) {
                Peers.update({ Peers.peerId eq peerId }) {
                    it[Peers.peerName] = peerName
                    it[Peers.lastOnline] = lastOnline
                }
            }
        }
    }

    suspend fun getAllPeers(): List<Triple<String, String, Long>> = dbQuery {
        Peers.selectAll().map {
            Triple(it[Peers.peerId], it[Peers.peerName], it[Peers.lastOnline])
        }
    }

    suspend fun saveMessage(
        peerId: String,
        senderId: String,
        senderName: String,
        content: String,
        timestamp: Long,
        messageId: String,
        isPending: Boolean = false
    ) = dbQuery {
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

    suspend fun getHistory(peerId: String): List<ChatHistoryMessage> = dbQuery {
        Messages.selectAll().where { Messages.peerId eq peerId }
            .orderBy(Messages.timestamp to SortOrder.ASC)
            .map {
                ChatHistoryMessage(
                    messageId = it[Messages.messageId],
                    senderId = it[Messages.senderId],
                    senderName = it[Messages.senderName],
                    content = it[Messages.content],
                    timeSend = it[Messages.timestamp],
                    isPending = it[Messages.isPending]
                )
            }
    }

    suspend fun markAsSent(messageId: String) = dbQuery {
        Messages.update({ Messages.messageId eq messageId }) {
            it[Messages.isPending] = false
        }
    }

    suspend fun getPendingMessages(): List<Pair<String, ChatHistoryMessage>> = dbQuery {
        Messages.selectAll().where { Messages.isPending eq true }
            .orderBy(Messages.timestamp to SortOrder.ASC)
            .map {
                val peerId = it[Messages.peerId]
                val msg = ChatHistoryMessage(
                    messageId = it[Messages.messageId],
                    senderId = it[Messages.senderId],
                    senderName = it[Messages.senderName],
                    content = it[Messages.content],
                    timeSend = it[Messages.timestamp],
                    isPending = it[Messages.isPending]
                )
                peerId to msg
            }
    }
}
