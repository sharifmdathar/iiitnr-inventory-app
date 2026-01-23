plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.compose.desktop)
}

kotlin {
    jvm()

    sourceSets {
        val jvmMain by getting {
            dependencies {
                implementation(project(":shared"))
                implementation(compose.desktop.currentOs)
            }
        }
    }
}

compose.desktop {
    application {
        mainClass = "com.iiitnr.inventoryapp.desktop.MainKt"

        nativeDistributions {
            val buildInstallers =
                project.findProperty("buildInstallers")?.toString()?.toBoolean() ?: false
            if (buildInstallers) {
                targetFormats(
                    org.jetbrains.compose.desktop.application.dsl.TargetFormat.Msi,
                    org.jetbrains.compose.desktop.application.dsl.TargetFormat.Exe
                )
            }

            packageName = "IIITNR Inventory App"
            packageVersion = "1.0.0"

            description = "IIITNR Inventory Management Application"
            vendor = "IIITNR"

            windows {
                val icon = project.file("icon.ico")
                if (icon.exists()) {
                    iconFile.set(icon)
                }
            }
        }
    }
}
