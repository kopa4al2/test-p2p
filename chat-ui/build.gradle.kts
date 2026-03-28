plugins {
    id("application")
    id("org.openjfx.javafxplugin") version "0.0.14"
    kotlin("jvm")
}

javafx {
    version = "23"
    modules = listOf("javafx.controls", "javafx.fxml")
}

application {
    mainClass.set("org.stefan.chatui.chat.MainChatWindow")
//    mainClass.set("org.stefan.chatui.ChatUiApplication")
}

dependencies {
    implementation("org.controlsfx:controlsfx:11.1.2")

    implementation("org.kordamp.ikonli:ikonli-materialdesign-pack:12.4.0")
    implementation("org.kordamp.ikonli:ikonli-javafx:12.4.0")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-javafx:1.8.0")

    // Any additional shared dependencies
    implementation(project(":chat-network"))
    implementation(project(":chat-common"))
}
repositories {
    mavenCentral()
}
kotlin {
    jvmToolchain(23)
}