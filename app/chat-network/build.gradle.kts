plugins {
    id("buildsrc.convention.kotlin-jvm")
    `java-library`
}

dependencies {
    api(project(":chat-common"))
    api(libs.kotlinx.coroutines.core)
}
