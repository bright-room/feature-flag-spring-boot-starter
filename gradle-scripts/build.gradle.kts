plugins {
    `kotlin-dsl`
}

dependencies {
    implementation(libs.spring.boot)
    implementation(libs.spring.dependency.management)

    implementation(libs.sonatype.central.upload)
    implementation(libs.spotless)

    // hack to access version catalogue https://github.com/gradle/gradle/issues/15383
    implementation(files(libs.javaClass.superclass.protectionDomain.codeSource.location))
}
