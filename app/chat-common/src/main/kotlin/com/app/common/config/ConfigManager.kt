package com.app.common.config

import java.io.File
import java.util.*

object ConfigManager {
    private const val APP_PATH = "app"
    private const val FILE_NAME = "config.properties"
    lateinit var current: AppConfig
        private set

    fun load() {
        val props = Properties()
        val file = File("$APP_PATH/$FILE_NAME")

        if (!file.exists()) {
            println("File ${file.absolutePath} not found, creating default config file...")
            val defaultId = UUID.randomUUID().toString()
            file.writeText(
                "user.id=$defaultId\nuser.name=${System.getenv("user.name")}\nstun.address=localhost\nstun.port=5000"
            )
        }

        println("Loading config from ${file.absolutePath}...")
        file.inputStream().use { props.load(it) }

        current = AppConfig(
            userId = getProperty("user.id", props),
            userName = getProperty("user.name", props),
            stunAddress = getProperty("stun.address", props, "localhost"),
            stunPort = getProperty("stun.port", props, "5000").toInt()
        )
    }

    private fun getProperty(key: String, props: Properties, defaultValue: String? = null): String {
        // 1. JVM Arguments (-Dkey=value)
        System.getProperty("p2p.$key")?.let { return it }

        // 2. Environment Variables (e.g. USER_NAME or user.name)
        System.getenv(key)?.let { return it }
        val envKey = key.replace('.', '_').uppercase()
        System.getenv(envKey)?.let { return it }

        // 3. Config file
        props.getProperty(key)?.let { return it }

        return defaultValue ?: throw IllegalStateException("Config key $key is missing and no default value provided.")
    }
}
