package util

import org.gradle.api.provider.Property

infix fun <T : Any> Property<T>.by(value: T?) {
    set(value)
}
