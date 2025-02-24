plugins {
    id("spring-boot-starter")
    id("publish-plugin")
    id("spotless-java")
}

description = "Library to integrate Spring MVC and Feature flags."

dependencies {
    implementation(project(":core"))

    implementation(libs.spring.boot.starter.web)
    testImplementation(libs.rest.assured)
}
