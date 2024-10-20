import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.jetbrains.compose)
    alias(libs.plugins.compose.compiler)
}

group = "com.retheviper"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    google()
}

dependencies {
    implementation(compose.desktop.currentOs)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose.desktop)
    implementation(libs.kotlinx.coroutines.swing)
    implementation(libs.koin.core)
    implementation(libs.koin.compose)
    implementation(libs.filekit.core)
    implementation(libs.filekit.compose)
    implementation(libs.junrar)
    implementation(libs.scrimage.core)
    implementation(libs.scrimage.webp)
    implementation(libs.slf4j.simple)
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