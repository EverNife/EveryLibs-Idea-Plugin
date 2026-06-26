import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.jetbrains.intellij.platform.gradle.TestFrameworkType

plugins {
    id("java")
    id("org.jetbrains.intellij.platform") version "2.16.0"
}

group = "dev.petrus.everylibs"
version = "1.0.0"

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

// IntelliJ 2024.2 runs on a JDK 17 runtime; compile and test against Java 17.
java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
}

dependencies {
    intellijPlatform {
        // Target a recent IDE that ships the Java module the contributor depends on.
        intellijIdeaCommunity("2024.2")
        bundledPlugin("com.intellij.java")
        testFramework(TestFrameworkType.Platform)
        // LightJavaCodeInsightFixtureTestCase lives in the Java plugin's test framework.
        testFramework(TestFrameworkType.Plugin.Java)
    }
    // The platform test framework drives JUnit 3/4 TestCase subclasses.
    testImplementation("junit:junit:4.13.2")
    // The platform's assertions reference opentest4j, which it does not export to the test classpath.
    testImplementation("org.opentest4j:opentest4j:1.3.0")
}

intellijPlatform {
    pluginConfiguration {
        ideaVersion {
            // 242 == 2024.2. Leave untilBuild open so the plugin keeps loading on newer IDEs.
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
