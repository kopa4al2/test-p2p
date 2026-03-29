package com.app.common.config

import java.io.File
import java.util.*

object ConfigLoader {
    fun load(): Properties {
        val props = Properties()
        val configFile = File("config.properties")
        
        if (configFile.exists()) {
            configFile.inputStream().use { props.load(it) }
        } else {
            createDefaultConfig(configFile)
        }
        return props
    }

    private fun createDefaultConfig(file: File) {
        file.writeText("""
            user.id=${UUID.randomUUID()}
            user.name=NewUser
            stun.server=localhost
            stun.port=5000
            last.chat.peer=
        """.trimIndent())
    }
}
