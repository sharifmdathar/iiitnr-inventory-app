plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ktlint)
    alias(libs.plugins.detekt)
}

android {
    namespace = "com.iiitnr.inventoryapp"
    compileSdk = 36

    val hasReleaseSigning =
        project.hasProperty("RELEASE_KEYSTORE_PATH") ||
            !(
                System.getenv("RELEASE_KEYSTORE_PATH")
                    ?: ""
                ).isBlank()
    if (hasReleaseSigning) {
        signingConfigs {
            create("release") {
                storeFile =
                    file(
                        (project.findProperty("RELEASE_KEYSTORE_PATH") as String?)
                            ?: System.getenv("RELEASE_KEYSTORE_PATH")!!,
                    )
                storePassword = project.findProperty("RELEASE_STORE_PASSWORD")?.toString()
                    ?: System.getenv("RELEASE_STORE_PASSWORD") ?: ""
                keyAlias = project.findProperty("RELEASE_KEY_ALIAS")?.toString()
                    ?: System.getenv("RELEASE_KEY_ALIAS") ?: ""
                keyPassword = project.findProperty("RELEASE_KEY_PASSWORD")?.toString()
                    ?: System.getenv("RELEASE_KEY_PASSWORD") ?: storePassword
            }
        }
    }

    defaultConfig {
        applicationId = "com.iiitnr.inventoryapp"
        minSdk = 24
        multiDexEnabled = true
        targetSdk = 36
        versionCode = 170
        versionName = "1.8.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            signingConfig =
                if (hasReleaseSigning) {
                    signingConfigs.getByName("release")
                } else {
                    signingConfigs.getByName("debug")
                }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
    }
}

detekt {
    buildUponDefaultConfig = true
    allRules = false
    config.setFrom(files("$rootDir/detekt.yml"))
    baseline = file("$rootDir/detekt-baseline.xml")
}

dependencies {
    implementation(project(":shared"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)

    implementation(libs.play.services.auth)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
