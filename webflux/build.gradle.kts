plugins {
    id("spring-boot-starter")
    id("publish-plugin")
    id("spotless-java")
    id("integration-test")
}

description = "Library to integrate Spring Reactive and Feature flags."

dependencies {
    implementation(projects.core)
    implementation(libs.spring.boot.starter.webflux)
    implementation(libs.spring.boot.starter.aspectj)

    testImplementation(libs.spring.boot.starter.webflux.test)

    integrationTestImplementation(libs.spring.boot.starter.webflux.test)
    integrationTestImplementation(libs.jsoup)
}
