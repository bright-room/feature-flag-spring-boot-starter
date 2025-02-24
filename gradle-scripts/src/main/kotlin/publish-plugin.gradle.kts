import util.by

plugins {
    `maven-publish`
    id("cl.franciscosolis.sonatype-central-upload")
}

publishing {
    publications {

        create<MavenPublication>("mavenJava") {
            from(components["java"])

            groupId = project.group.toString()
            artifactId = project.name

            versionMapping {
                usage("java-api") {
                    fromResolutionOf("runtimeClasspath")
                }
                usage("java-runtime") {
                    fromResolutionResult()
                }
            }

            pom {
                name by project.name
                description by project.description
                url by "https://github.com/bright-room/feature-flag-spring-boot-starter"

                issueManagement {
                    url by "https://github.com/bright-room/feature-flag-spring-boot-starter/issues"
                }

                licenses {
                    license {
                        name by "MIT License"
                        url by "https://api.github.com/licenses/mit"
                        description by "repo"
                    }
                }

                developers {
                    developer {
                        id by "kukv"
                        name by "Koki Nonaka"
                    }
                }

                scm {
                    connection by "scm:git:git://github.com/bright-room/feature-flag-spring-boot-starter.git"
                    developerConnection by "scm:git:git@github.com:bright-room/feature-flag-spring-boot-starter.git"
                    url by "https://github.com/bright-room/feature-flag-spring-boot-starter"
                }
            }
        }
    }
    repositories {
        maven {
            setUrl(layout.buildDirectory.dir("staging-deploy"))
        }
    }
}

tasks {
    sonatypeCentralUpload {
        dependsOn("jar", "sourcesJar", "javadocJar", "generatePomFileForMavenJavaPublication")

        username = System.getenv("SONATYPE_CENTRAL_USERNAME")
        password = System.getenv("SONATYPE_CENTRAL_PASSWORD")

        archives =
            files(
                tasks.named("jar"),
                tasks.named("sourcesJar"),
                tasks.named("javadocJar"),
            )

        pom =
            file(
                tasks
                    .named("generatePomFileForMavenJavaPublication")
                    .get()
                    .outputs.files
                    .single(),
            )

        signingKey = System.getenv("PGP_SIGNING_KEY")
        signingKeyPassphrase = System.getenv("PGP_SIGNING_KEY_PASSPHRASE")
    }
}
