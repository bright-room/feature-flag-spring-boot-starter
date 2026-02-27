plugins {
    java
}

sourceSets {
    create("integrationTest") {
        compileClasspath += sourceSets.main.get().output
        runtimeClasspath += sourceSets.main.get().output
    }
}

val integrationTestImplementation by configurations.getting {
    extendsFrom(configurations.testImplementation.get())
}

val integrationTestRuntimeOnly by configurations.getting {
    extendsFrom(configurations.testRuntimeOnly.get())
}

tasks {
    val integrationTest =
        register<Test>("integrationTest") {
            description = "Runs integration tests."
            group = "verification"
            testClassesDirs = sourceSets["integrationTest"].output.classesDirs
            classpath = sourceSets["integrationTest"].runtimeClasspath
            useJUnitPlatform()
            shouldRunAfter(test)
        }

    check {
        dependsOn(integrationTest)
    }
}
