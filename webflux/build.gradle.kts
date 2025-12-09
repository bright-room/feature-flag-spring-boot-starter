plugins {
    id("spring-boot-starter")
    id("publish-plugin")
    id("spotless-java")
}

description = "Library to integrate Spring Reactive and Feature flags."

dependencies {
    implementation(project(":core"))
    implementation(libs.spring.boot.starter.webflux)

    testImplementation(libs.spring.boot.starter.webflux.test)
}
