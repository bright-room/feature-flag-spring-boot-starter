plugins {
    id("spring-boot-starter")
    id("publish-plugin")
    id("spotless-java")
}

description = "Core library to make feature flags easily available in spring boot."

dependencies {
    implementation(libs.spring.boot.starter)
}
