plugins {
    id("com.diffplug.spotless")
}

spotless {
    kotlin {
        ktlint()
        target("gradle-scripts/**/*.kt")
        targetExclude("**/build/**")
    }
    kotlinGradle {
        ktlint()
        target("**/*.gradle.kts")
        targetExclude("**/build/**")
    }
}
