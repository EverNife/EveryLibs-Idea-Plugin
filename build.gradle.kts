import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.jetbrains.intellij.platform.gradle.TestFrameworkType

plugins {
    // This is a Java plugin (the production sources are java.lang.reflect / PSI code), so the
    // template's Kotlin JVM plugin is intentionally swapped for the `java` plugin.
    id("java")
    // Version inherited from the `org.jetbrains.intellij.platform.settings` plugin in settings.gradle.kts.
    id("org.jetbrains.intellij.platform")
    // Patches the plugin's change-notes from CHANGELOG.md; auto-wired by the IntelliJ Platform plugin.
    id("org.jetbrains.changelog")
}

// `group` and `version` are applied from gradle.properties automatically.

// IntelliJ 2024.2 ships and runs on JetBrains Runtime 21; compile and test against Java 21
// so the plugin matches the platform's bytecode/API level.
java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

dependencies {
    // The platform test framework drives the LightJavaCodeInsightFixtureTestCase suite as JUnit.
    testImplementation("junit:junit:4.13.2")
    // The platform's assertions reference opentest4j, which it does not export to the test classpath
    // on the 242 line. (Bundled from 251+, so this becomes redundant if the target is bumped.)
    testImplementation("org.opentest4j:opentest4j:1.3.0")

    // IntelliJ Platform Gradle Plugin Dependencies Extension:
    // https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin-dependencies-extension.html
    intellijPlatform {
        // Target a recent IDE that ships the Java module the contributor depends on.
        intellijIdeaCommunity("2024.2")
        bundledPlugin("com.intellij.java")
        testFramework(TestFrameworkType.Platform)
        // LightJavaCodeInsightFixtureTestCase lives in the Java plugin's test framework.
        testFramework(TestFrameworkType.Plugin.Java)
    }
}

intellijPlatform {
    pluginConfiguration {
        ideaVersion {
            // 242 == 2024.2. Leave untilBuild open so the plugin keeps loading on newer IDEs
            // instead of being pinned to the build it was compiled against.
            sinceBuild = "242"
            untilBuild = provider { null }
        }
    }
}

tasks {
    test {
        // The platform test framework drives these as JUnit; keep them on the 17 runtime.
        useJUnit()
        testLogging {
            events = setOf(TestLogEvent.PASSED, TestLogEvent.FAILED, TestLogEvent.SKIPPED)
            exceptionFormat = TestExceptionFormat.FULL
            showStackTraces = true
            showCauses = true
        }
    }
}
