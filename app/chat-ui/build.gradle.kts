plugins {
    kotlin("jvm")
    id("application")
    alias(libs.plugins.javafx.gradle)
    alias(libs.plugins.gluonfx.gradle)
}

application {
    mainClass.set("com.app.ui.MainChatWindow")
}

javafx {
    version = "23"
    modules = listOf("javafx.controls", "javafx.fxml")
}

gluonfx {
    graalvmHome = System.getenv("GRAALVM_HOME")
}

dependencies {
    implementation(libs.controlsfx)

    implementation(libs.bundles.ikonli)

    implementation(libs.kotlinx.coroutines.javafx)

    implementation(project(":chat-network"))
    implementation(project(":chat-common"))
}
repositories {
    mavenCentral()
}
kotlin {
    jvmToolchain(23)
}
