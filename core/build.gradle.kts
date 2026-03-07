plugins {
    id("spring-boot-starter")
    id("publish-plugin")
    id("spotless-java")
}

description = "Core Library to Feature flags."

dependencies {
    implementation(libs.spring.boot.autoconfigure)
    implementation(libs.spring.web)
    compileOnly(libs.reactor.core)
    testImplementation(libs.spring.boot.starter.test)
    testImplementation(libs.reactor.core)
    testImplementation(libs.reactor.test)
}
