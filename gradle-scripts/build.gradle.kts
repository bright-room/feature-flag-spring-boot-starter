plugins {
    `kotlin-dsl`
}

dependencies {
    implementation(libs.sonatype.central.upload)

    // hack to access version catalogue https://github.com/gradle/gradle/issues/15383
    implementation(files(libs.javaClass.superclass.protectionDomain.codeSource.location))
}
