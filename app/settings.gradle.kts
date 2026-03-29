dependencyResolutionManagement {
    @Suppress("UnstableApiUsage")
    repositories {
        mavenCentral()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}

include(":chat-ui")
include(":chat-network")
include(":chat-common")

include(":utils")
rootProject.name = "app"
