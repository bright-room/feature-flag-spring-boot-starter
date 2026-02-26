import util.libs

plugins {
    java
    id("org.springframework.boot")
    id("io.spring.dependency-management")
}

group = "net.bright-room.feature-flag-spring-boot-starter"
version = libs.versions.app.get()

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
    }
}
