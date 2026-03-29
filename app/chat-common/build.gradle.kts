plugins {
    kotlin("jvm")
    alias(libs.plugins.kotlin.serialization)
    `java-library`
}

group = "org.stefan"
version = "0.0.1"

repositories {
    mavenCentral()
}

dependencies {
    api(libs.kotlinx.serialization.json)
    api(libs.exposed.core)
    api(libs.exposed.jdbc)
    api(libs.exposed.dao)
    api(libs.exposed.datetime)
    api(libs.sqlite.jdbc)
    api(libs.h2)
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(22)
}