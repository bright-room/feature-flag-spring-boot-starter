plugins {
    java
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.spring.dependency.management)
    alias(libs.plugins.spotless)
}

description = "Core Library to Feature flags."
group = "net.bright-room.feature-flag-spring-boot-starter"
version = libs.versions.app.get()

dependencies {
    implementation(libs.spring.boot.starter)
    annotationProcessor(libs.spring.boot.configuration.processor)

    testImplementation(libs.spring.boot.starter.test)
    testRuntimeOnly(libs.junit.platform.launcher)
}

java {
    withJavadocJar()
    withSourcesJar()
    toolchain {
        languageVersion = JavaLanguageVersion.of(libs.versions.java.get())
    }
}

spotless {
    java {
        googleJavaFormat()
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
