import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    kotlin("jvm")
    id("org.jetbrains.compose")
    id("org.jetbrains.kotlin.plugin.compose")
}

group = "com.retheviper"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    google()
}

dependencies {
    // Note, if you develop a library, you should use compose.desktop.common.
    // compose.desktop.currentOs should be used in launcher-sourceSet
    // (in a separate module for demo project and in testMain).
    // With compose.desktop.common you will also lose @Preview functionality
    implementation(compose.desktop.currentOs)
    implementation("org.jetbrains.androidx.lifecycle:lifecycle-viewmodel-compose:2.8.0")
    implementation("org.jetbrains.androidx.navigation:navigation-compose-desktop:2.8.0-alpha10")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-swing:1.9.0-RC")
    implementation("io.insert-koin:koin-core:4.0.0")
    implementation("io.insert-koin:koin-compose:4.0.0")
    implementation("io.github.vinceglb:filekit-core-jvm:0.8.7")
    implementation("io.github.vinceglb:filekit-compose:0.8.7")
    implementation("com.github.junrar:junrar:7.5.5")
    implementation("com.sksamuel.scrimage:scrimage-core:4.2.0")
    implementation("com.sksamuel.scrimage:scrimage-webp:4.2.0")
    implementation("org.slf4j:slf4j-simple:2.0.13")
}

compose.desktop {
    application {
        mainClass = "MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "Junkyard"
            packageVersion = "1.0.0"

            macOS {
                iconFile = file("src/main/resources/icons/Junkyard.icns")
                bundleID = "com.retheviper.junkyard"
                dockName = "Junkyard"
                appCategory = "public.app-category.utilities"
            }
            windows {
                iconFile = file("src/main/resources/icons/Junkyard.ico")
                dirChooser = true
                upgradeUuid = "50be04cb-ea58-423a-8a41-ed69ce332700"
            }
            linux {
                iconFile = file("src/main/resources/icons/Junkyard.png")
            }
        }
    }
}