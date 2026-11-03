import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    kotlin("jvm")
    alias(libs.plugins.jetbrains.compose)
    alias(libs.plugins.compose.compiler)
}

dependencies {
    implementation(project(":core"))
    implementation(project(":audio"))

    implementation(compose.desktop.currentOs)
    implementation(compose.material3)
    implementation(compose.materialIconsExtended)
    implementation(compose.components.resources)

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-swing:1.9.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("org.json:json:20240303")

    // JavaFX Media Engine for Windows native audio streaming (Windows Media Foundation backend)
    implementation("org.openjfx:javafx-base:21.0.2:win")
    implementation("org.openjfx:javafx-graphics:21.0.2:win")
    implementation("org.openjfx:javafx-media:21.0.2:win")
}

compose.desktop {
    application {
        mainClass = "com.uta.wave.desktop.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Msi, TargetFormat.Exe)
            packageName = "UTA-wave"
            packageVersion = "1.0.0"
            description = "UTA-WAVE - A Universe of Music (Windows Desktop Client)"
            copyright = "© 2026 UTA-VERSE. All rights reserved."
            vendor = "UTA-VERSE"

            windows {
                menuGroup = "UTA-VERSE"
                upgradeUuid = "68c2d1b4-2b7e-4b2a-8951-6d7ef902b78a"
                perUserInstall = true
                shortcut = true
            }
        }
    }
}

kotlin {
    jvmToolchain(17)
}


