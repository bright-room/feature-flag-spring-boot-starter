plugins {
    id("spring-boot-starter")
    id("publish-plugin")
    id("spotless-java")
    id("integration-test")
}

description = "Library to integrate Spring MVC and Feature flags."

dependencies {
    implementation(project(":core"))
    implementation(libs.spring.boot.starter.webmvc)

    testImplementation(libs.spring.boot.starter.webmvc.test)

    integrationTestImplementation(libs.spring.boot.starter.thymeleaf)
    integrationTestImplementation(libs.jsoup)
}
