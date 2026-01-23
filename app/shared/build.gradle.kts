plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.android.kotlin.multiplatform.library)
}

kotlin {
    androidLibrary {
        namespace = "com.iiitnr.inventoryapp.shared"
        compileSdk = 36
    }

    iosX64()
    iosArm64()
    iosSimulatorArm64()

    jvm()

    sourceSets {
        val commonMain by getting {
            dependencies {
                val composeVersion = libs.versions.composeMultiplatform.get()

                implementation("org.jetbrains.compose.runtime:runtime:$composeVersion")
                implementation("org.jetbrains.compose.ui:ui:$composeVersion")
                implementation("org.jetbrains.compose.foundation:foundation:$composeVersion")

                implementation("org.jetbrains.compose.material3:material3:1.10.0-alpha05")

                implementation(libs.ktor.client.core)
                implementation(libs.ktor.client.content.negotiation)
                implementation(libs.ktor.serialization.kotlinx.json)
                implementation(libs.ktor.client.logging)

                implementation(libs.kotlinx.serialization.json)

                implementation(libs.kotlinx.coroutines.core)

                implementation("org.jetbrains.androidx.navigation:navigation-compose:2.9.1")
            }
        }

        val androidMain by getting {
            dependencies {
                implementation(libs.ktor.client.android)
                implementation(libs.androidx.activity.compose)
                implementation(libs.androidx.datastore.preferences)
            }
        }

        val iosX64Main by getting {
            dependencies {
                implementation(libs.ktor.client.darwin)
            }
        }
        val iosArm64Main by getting {
            dependencies {
                implementation(libs.ktor.client.darwin)
            }
        }
        val iosSimulatorArm64Main by getting {
            dependencies {
                implementation(libs.ktor.client.darwin)
            }
        }

        val jvmMain by getting {
            dependencies {
                implementation(libs.ktor.client.cio)
            }
        }
    }
}
