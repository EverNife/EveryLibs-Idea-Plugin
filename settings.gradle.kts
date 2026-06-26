// Standalone build — deliberately NOT included in the root EveryLibs settings.gradle.
//
// The root reactor forces `options.release = 8` + Jabel on every subproject (a Java 8 API
// floor). An IntelliJ plugin targets the IDE runtime (JDK 17+) and cannot live under that
// constraint, so it is its own Gradle build that happens to sit inside the repo.
pluginManagement {
    repositories {
        gradlePluginPortal()
    }
}

rootProject.name = "everylibs-idea-plugin"
