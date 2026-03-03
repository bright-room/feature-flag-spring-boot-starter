plugins {
    id("spring-boot-starter")
    id("publish-plugin")
    id("spotless-java")
    id("integration-test")
}

description = "Library to integrate Spring Boot Actuator and Feature flags."

dependencies {
    implementation(projects.core)
    implementation(libs.spring.boot.starter.actuator)
    compileOnly(libs.reactor.core)

    testImplementation(libs.spring.boot.starter.test)
    testImplementation(libs.spring.boot.starter.webmvc)
    testImplementation(libs.spring.boot.starter.webflux)

    integrationTestImplementation(libs.spring.boot.starter.test)
    integrationTestImplementation(libs.spring.boot.starter.webmvc)
    integrationTestImplementation(libs.spring.boot.starter.webmvc.test)
    integrationTestImplementation(libs.spring.boot.starter.webflux)
    integrationTestImplementation(libs.spring.boot.starter.webflux.test)
}
