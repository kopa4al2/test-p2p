plugins {
    id("buildsrc.convention.kotlin-jvm")
    `java-library`
}

dependencies {
    api(project(":chat-common"))
    api("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.0")
}
