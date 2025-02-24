import util.libs

plugins {
    java
    id("org.springframework.boot")
    id("io.spring.dependency-management")
}

group = "net.bright-room.feature-flag-spring-boot-starter"
version = "0.0.3"

dependencies {
    annotationProcessor(libs.spring.boot.configuration.processor)
    testImplementation(libs.spring.boot.starter.test)
}

val javaVersion: String = libs.versions.java.get()
java {
    withJavadocJar()
    withSourcesJar()
    toolchain {
        languageVersion = JavaLanguageVersion.of(javaVersion)
    }
}

tasks {
    compileJava {
        sourceCompatibility = javaVersion
        targetCompatibility = javaVersion
    }

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
