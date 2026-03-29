package com.app.common.config

import java.io.File
import java.util.*

object ConfigManager {
    private const val APP_PATH = "src/main/resources"
    private const val FILE_NAME = "config.properties"
    lateinit var current: AppConfig
        private set

    private val defaultProperties: Map<String, () -> String> = mapOf(
        "user.id" to { UUID.randomUUID().toString() },
        "user.name" to { System.getProperty("user.name") ?: "Unknown" },
        "stun.address" to { "localhost" },
        "stun.port" to { "5000" }
    )

    fun load() {
        val props = Properties()
        val file = File("$APP_PATH/$FILE_NAME")

        if (!file.exists()) {
            file.parentFile.mkdirs()
            println("File ${file.absolutePath} not found, creating default config file...")
            
            val defaults = Properties()
            defaultProperties.forEach { (key, provider) ->
                defaults.setProperty(key, provider())
            }
            
            file.outputStream().use { 
                defaults.store(it, "Default configuration for P2P Chat")
            }
        }

        println("Loading config from ${file.absolutePath}...")
        file.inputStream().use { props.load(it) }

        current = AppConfig(
            userId = getProperty("user.id", props),
            userName = getProperty("user.name", props),
            stunAddress = getProperty("stun.address", props),
            stunPort = getProperty("stun.port", props).toInt()
        )
    }

    private fun getProperty(key: String, props: Properties): String {
        // 1. JVM Arguments (-Dkey=value)
        System.getProperty("p2p.$key")?.let { return it }

        // 2. Environment Variables (e.g. USER_NAME or user.name)
        System.getenv(key)?.let { return it }
        val envKey = key.replace('.', '_').uppercase()
        System.getenv(envKey)?.let { return it }

        // 3. Config file
        props.getProperty(key)?.let { return it }

        // 4. Default values
        return defaultProperties[key]?.invoke()
            ?: throw IllegalStateException("Config key $key is missing and no default value provided.")
    }
}
