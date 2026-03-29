package com.app.common.config

import java.io.File
import java.util.*

object ConfigManager {
    private const val FILE_NAME = "config.properties"
    lateinit var current: AppConfig
        private set

    fun load() {
        val props = Properties()
        val file = File(FILE_NAME)
        
        if (!file.exists()) {
            // Създай default файл, ако липсва
            val defaultId = UUID.randomUUID().toString()
            file.writeText("user.id=$defaultId\nuser.name=NewUser\nstun.address=localhost\nstun.port=5000")
        }
        
        file.inputStream().use { props.load(it) }
        
        current = AppConfig(
            userId = props.getProperty("user.id"),
            userName = props.getProperty("user.name"),
            stunAddress = props.getProperty("stun.address"),
            stunPort = props.getProperty("stun.port").toInt()
        )
    }
}
