plugins {
    kotlin("jvm") version "2.1.0"
    id("org.jetbrains.compose") version "1.7.1"
    id("org.jetbrains.kotlin.plugin.compose") version "2.1.0"
}

group = "com.zhang.adbhub"
version = "1.1.0"

dependencies {
    // Common module
    implementation(project(":common"))

    // Compose Desktop
    implementation(compose.desktop.currentOs)
    implementation(compose.material3)
    implementation(compose.materialIconsExtended)

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-swing:1.9.0")
}

kotlin {
    jvmToolchain(11)
}

compose.desktop {
    application {
        mainClass = "com.zhang.adbhub.desktop.MainKt"

        nativeDistributions {
            targetFormats(
                org.jetbrains.compose.desktop.application.dsl.TargetFormat.Dmg,
                org.jetbrains.compose.desktop.application.dsl.TargetFormat.Msi,
                org.jetbrains.compose.desktop.application.dsl.TargetFormat.Exe,
                org.jetbrains.compose.desktop.application.dsl.TargetFormat.Deb
            )
            packageName = "ADBHub"
            packageVersion = "1.1.0"
            description = "Android Debug Bridge 管理工具"
            vendor = "ADBHub Team"

            windows {
                // iconFile.set(project.file("src/main/resources/icon.ico"))
                menuGroup = "ADBHub"
                upgradeUuid = "18159732-6D3E-4F1A-8F6C-D8B5D9C8E7A1"
            }
        }
    }
}
