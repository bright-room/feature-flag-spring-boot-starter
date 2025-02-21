plugins {
    java
    id("org.springframework.boot")
    id("io.spring.dependency-management")
}

group = "net.bright-room.feature-flag-spring-boot-starter"
version = "0.0.1"

val javaVersion = JavaLanguageVersion.of("21")
java {
    toolchain {
        languageVersion = javaVersion
    }
}

tasks {
    compileJava {
        sourceCompatibility = javaVersion.toString()
        targetCompatibility = javaVersion.toString()
    }

    test {
        useJUnitPlatform()
    }
}
