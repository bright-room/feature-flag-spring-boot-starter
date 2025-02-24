import org.jreleaser.model.Active
import util.by

plugins {
    `maven-publish`
    id("org.jreleaser")
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

jreleaser {
    gitRootSearch = true

    project {
        inceptionYear = "2025"
        author("@kukv")
    }

    signing {
        active = Active.ALWAYS
        armored = true
        verify = true
    }

    release {
        github {
            skipRelease = true
            skipTag = true
            sign = true
            branch = "main"
            branchPush = "main"
            overwrite = true
        }
    }

    deploy {
        maven {
            mavenCentral.create("sonatype") {
                active = Active.ALWAYS
                url = "https://central.sonatype.com/api/v1/publisher"
                stagingRepository(layout.buildDirectory.dir("staging-deploy").get().toString())
                setAuthorization("Basic")
                retryDelay = 60
            }
        }
    }
}
