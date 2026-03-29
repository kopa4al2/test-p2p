package com.app.common.db

import org.jetbrains.exposed.sql.Table

object Messages : Table() {
    val id = integer("id").autoIncrement()
    val messageId = varchar("message_id", 36).uniqueIndex()
    val senderId = varchar("sender_id", 100)
    val senderName = varchar("sender_name", 100)
    val content = text("content")
    val timestamp = long("timestamp")
    val isPending = bool("is_pending").default(false)
    val peerId = varchar("peer_id", 100) // The ID of the peer we are chatting with

    override val primaryKey = PrimaryKey(id)
}
