plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.iiitnr.inventoryapp"
    compileSdk = 36

    if (project.hasProperty("RELEASE_KEYSTORE_PATH")) {
        signingConfigs {
            create("release") {
                storeFile = file(project.property("RELEASE_KEYSTORE_PATH") as String)
                storePassword = project.findProperty("RELEASE_STORE_PASSWORD")?.toString() ?: ""
                keyAlias = project.findProperty("RELEASE_KEY_ALIAS")?.toString() ?: ""
                keyPassword = project.findProperty("RELEASE_KEY_PASSWORD")?.toString() ?: storePassword
            }
        }
    }

    defaultConfig {
        applicationId = "com.iiitnr.inventoryapp"
        minSdk = 24
        multiDexEnabled = true
        targetSdk = 36
        versionCode = 160
        versionName = "1.6.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro"
            )
            signingConfig = if (project.hasProperty("RELEASE_KEYSTORE_PATH")) {
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

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}