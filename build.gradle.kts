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
    implementation("org.jetbrains.androidx.navigation:navigation-compose-desktop:2.8.0-alpha08")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-swing:1.9.0-RC")
    implementation("io.insert-koin:koin-core:3.6.0-alpha1")
    implementation("io.insert-koin:koin-compose:3.6.0-alpha1")
    implementation("io.github.vinceglb:filekit-core-jvm:0.7.0")
    implementation("io.github.vinceglb:filekit-compose:0.7.0")
    implementation("com.github.junrar:junrar:7.5.5")
    implementation("com.sksamuel.scrimage:scrimage-core:4.1.3")
    implementation("com.sksamuel.scrimage:scrimage-webp:4.1.3")
    implementation("org.slf4j:slf4j-simple:2.0.13")
}

compose.desktop {
    application {
        mainClass = "MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "junkyard"
            packageVersion = "1.0.0"

            macOS {
                iconFile = file("src/main/resources/icons/Junkyard.icns")
            }
            windows {
                iconFile = file("src/main/resources/icons/Junkyard.ico")
            }
        }
    }
}
