tasks.register("start") {
    dependsOn(":chat-ui:run")
}

tasks.register("nativeBuildWindows") {
    group = "build"
    description = "Builds the native application for Windows"
    doFirst {
        project.gradle.startParameter.projectProperties["gluonfx.target"] = "host"
    }
    finalizedBy(":chat-ui:nativeBuild")
}

tasks.register("nativeBuildAndroid") {
    group = "build"
    description = "Builds the native application for Android"
    doFirst {
        project.gradle.startParameter.projectProperties["gluonfx.target"] = "android"
    }
    finalizedBy(":chat-ui:nativeBuild")
}

tasks.matching { it.name == "nativeCompile" }.all {
    mustRunAfter(subprojects.map { it.tasks.matching { t -> t.name == "build" } })
    mustRunAfter(subprojects.map { it.tasks.matching { t -> t.name == "clean" } })
}

// 2. РЕГИСТРИРАМЕ ЗАДАЧАТА
tasks.register("buildOptimizedNative") {
    group = "gluonfx"

    // Вместо ':build', казваме на Gradle да пусне 'clean' и 'build' във ВСЕКИ подмодул
    dependsOn(subprojects.map { it.tasks.matching { t -> t.name == "clean" } })
    dependsOn(subprojects.map { it.tasks.matching { t -> t.name == "build" } })

    // И накрая специфичната задача за Native Image
    dependsOn(":chat-ui:nativeCompile")

    doLast {
        val exePath = layout.buildDirectory.file("gluonfx/x86_64-windows/chat-ui.exe").get().asFile
        if (exePath.exists()) {
            println("\n--- SUCCESS! Final size: ${exePath.length() / 1024 / 1024} MB ---")
        }
    }
}