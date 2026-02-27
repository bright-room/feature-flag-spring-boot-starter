plugins {
    id("spring-boot-starter")
    id("publish-plugin")
    id("spotless-java")
}

description = "Core Library to Feature flags."

dependencies {
    implementation(libs.spring.boot.autoconfigure)
    testImplementation(libs.spring.boot.starter.test)
}
