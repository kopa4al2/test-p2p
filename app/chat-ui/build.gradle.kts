plugins {
    kotlin("jvm")
    id("application")
    alias(libs.plugins.javafx.gradle)
    alias(libs.plugins.gluonfx.gradle)
}

application {
    mainClass.set("com.app.ui.Launcher")
}

javafx {
    version = "23"
    modules = listOf(
        "javafx.controls"
//        "javafx.fxml"
    )
}

gluonfx {
    graalvmHome = "C:\\Users\\Stefan\\.jdks\\graalvm-ce-23.0.2"
    target = project.findProperty("gluonfx.target")?.toString() ?: "host"
    linkerArgs = listOf("management_ext.lib", "psapi.lib")
    compilerArgs = listOf(
        "-H:+StripDebugInfo",
        "-H:+RemoveUnusedSymbols",
        "-H:IncludeLocales=en,bg",
        "--no-fallback"
    )
}

tasks.withType<JavaExec> {
}

dependencies {
    implementation(libs.controlsfx)

    implementation(libs.bundles.ikonli)

    implementation(libs.kotlinx.coroutines.javafx)
    implementation("ch.qos.logback:logback-classic:1.5.16")

    implementation(project(":chat-network"))
    implementation(project(":chat-common"))
}
repositories {
    gradlePluginPortal()
    mavenCentral()
}
kotlin {
    jvmToolchain(23)
}
