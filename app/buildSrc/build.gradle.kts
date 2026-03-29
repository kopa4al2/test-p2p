plugins {
    `kotlin-dsl`
}

kotlin {
    jvmToolchain(22)
}

dependencies {
    implementation(libs.kotlin.gradle.plugin)
}
