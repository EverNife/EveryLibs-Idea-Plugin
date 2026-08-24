// Standalone build — deliberately NOT included in the root EveryLibs settings.gradle.
//
// The root reactor forces `options.release = 8` + Jabel on every subproject (a Java 8 API
// floor). An IntelliJ plugin targets the IDE runtime (JDK 17+) and cannot live under that
// constraint, so it is its own Gradle build that happens to sit inside the repo.
//
// Structured after the official IntelliJ Platform Plugin Template: repository configuration
// lives here (via the IntelliJ Platform repositories extension), and Gradle plugin versions
// are pinned in `pluginManagement` so the build script applies them without versions.
import org.jetbrains.intellij.platform.gradle.extensions.intellijPlatform

rootProject.name = "everylibs-idea-plugin"

pluginManagement {
    repositories {
        gradlePluginPortal()
    }
    plugins {
        id("org.jetbrains.changelog") version "2.5.0"
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
    id("org.jetbrains.intellij.platform.settings") version "2.18.1"
}

@Suppress("UnstableApiUsage")
dependencyResolutionManagement {
    repositories {
        mavenCentral()

        // IntelliJ Platform Gradle Plugin Repositories Extension:
        // https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin-repositories-extension.html
        intellijPlatform {
            defaultRepositories()
        }
    }
}
