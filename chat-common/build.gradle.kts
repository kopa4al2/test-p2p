plugins {
    kotlin("jvm")
    kotlin("plugin.serialization") version "2.0.0"
    `java-library`
}

group = "org.stefan"
version = "0.0.1"

repositories {
    mavenCentral()
}

dependencies {
    api("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(23)
}