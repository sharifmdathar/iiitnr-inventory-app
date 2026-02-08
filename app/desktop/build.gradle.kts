import java.util.Properties
import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.compose.desktop)
}

kotlin {
    jvm()

    sourceSets {
        getByName("jvmMain") {
            dependencies {
                implementation(project(":shared"))
                implementation(compose.desktop.currentOs)
                implementation(libs.ktor.client.cio)
            }
            resources.srcDir("src/jvmMain/resources")
        }
    }
}

tasks.register("populateGoogleConfig") {
    doLast {
        val propertiesFile = project.file("src/jvmMain/resources/google-desktop-config.properties")
        val properties = Properties()

        if (propertiesFile.exists()) {
            propertiesFile.inputStream().use { stream ->
                properties.load(stream)
            }
        }

        val clientId = System.getenv("GOOGLE_DESKTOP_CLIENT_ID")
        if (clientId != null && clientId.isNotBlank()) {
            properties.setProperty("google.desktop.client.id", clientId)
        }

        val clientSecret = System.getenv("GOOGLE_DESKTOP_CLIENT_SECRET")
        if (clientSecret != null && clientSecret.isNotBlank()) {
            properties.setProperty("google.desktop.client.secret", clientSecret)
        }

        val redirectUri = System.getenv("GOOGLE_DESKTOP_REDIRECT_URI")
        if (redirectUri != null && redirectUri.isNotBlank()) {
            properties.setProperty("google.desktop.redirect.uri", redirectUri)
        }

        propertiesFile.parentFile.mkdirs()
        propertiesFile.outputStream().use { stream ->
            properties.store(stream, "Google Desktop OAuth Configuration - Auto-generated from environment variables")
        }
    }
}

tasks.named("jvmProcessResources") {
    dependsOn("populateGoogleConfig")
}

compose.desktop {
    application {
        mainClass = "com.iiitnr.inventoryapp.desktop.MainKt"

        nativeDistributions {
            val buildInstallers =
                project.findProperty("buildInstallers")?.toString()?.toBoolean() ?: false
            val osName = System.getProperty("os.name").lowercase()
            val formats = mutableListOf<TargetFormat>()
            if (buildInstallers) {
                if (osName.contains("win")) {
                    formats.add(TargetFormat.Msi)
                    formats.add(TargetFormat.Exe)
                }
            }
            if (osName.contains("linux")) {
                formats.add(TargetFormat.AppImage)
            }
            if (formats.isNotEmpty()) {
                targetFormats(*formats.toTypedArray())
            }

            packageName = "IIITNR Inventory App"
            packageVersion = "1.11.0"

            description = "IIITNR Inventory Management Application"
            vendor = "IIITNR"
            includeAllModules = true

            windows {
                val icon = project.file("icon.ico")
                if (icon.exists()) {
                    iconFile.set(icon)
                }
                menu = true
                shortcut = true
            }

            linux {
                val icon = project.file("icon.png")
                if (icon.exists()) {
                    iconFile.set(icon)
                }
            }
        }
    }
}
