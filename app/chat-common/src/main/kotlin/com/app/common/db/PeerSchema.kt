package com.app.common.db

import org.jetbrains.exposed.sql.Table

object Peers : Table() {
    val id = integer("id").autoIncrement()
    val peerId = varchar("peer_id", 36).uniqueIndex()
    val peerName = varchar("peer_name", 100)
    val lastOnline = long("last_online")

    override val primaryKey = PrimaryKey(id)
}
