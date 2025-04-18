plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.10.0"
}

@Suppress("UnstableApiUsage")
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

rootProject.name = "feature-flag-spring-boot-starter"
include("core")
include("web")
include("webflux")

includeBuild("gradle-scripts")
