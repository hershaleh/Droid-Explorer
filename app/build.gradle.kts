plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.droidexplorer.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.droidexplorer"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "0.0.1"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation(project(":core:capabilities"))
    implementation(project(":ui:design"))
    implementation(project(":feature:files"))
    implementation(project(":storage:api"))
    implementation(project(":storage:local"))
    implementation(libs.androidx.activity.compose)
}
