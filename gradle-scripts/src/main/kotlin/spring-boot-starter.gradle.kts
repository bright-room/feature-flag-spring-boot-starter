import util.libs

plugins {
    java
    id("org.springframework.boot")
    id("io.spring.dependency-management")
}

group = "net.bright-room.feature-flag-spring-boot-starter"
version = providers.gradleProperty("releaseVersion").getOrElse("0.0.0-SNAPSHOT")

dependencies {
    annotationProcessor(libs.spring.boot.configuration.processor)
    testRuntimeOnly(libs.junit.platform.launcher)
}

java {
    withJavadocJar()
    withSourcesJar()
    toolchain {
        languageVersion = JavaLanguageVersion.of(libs.versions.java.get())
    }
}

tasks {
    bootJar {
        enabled = false
    }

    jar {
        archiveClassifier.set("")
    }

    test {
        useJUnitPlatform()
        maxParallelForks = (Runtime.getRuntime().availableProcessors() / 2).coerceAtLeast(1)
    }
}
