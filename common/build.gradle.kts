plugins {
    kotlin("jvm") version "2.1.0"
    kotlin("plugin.serialization") version "2.1.0"
}

group = "com.zhang.adbhub"
version = "1.0.0"

dependencies {
    // Dadb - Kotlin ADB library
    implementation("dev.mobile:dadb:1.2.9")

    // Kotlin Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")

    // Kotlin Serialization
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")

}

kotlin {
    jvmToolchain(11)
}
