plugins {
    id("com.diffplug.spotless")
}

spotless {
    java {
        googleJavaFormat()
    }
}
