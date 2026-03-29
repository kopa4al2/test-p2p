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