plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("dagger.hilt.android.plugin")  // Added for Hilt support
    kotlin("kapt")                   // Added for annotation processing
}


android {
    namespace = "com.example.myrides"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.myrides"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
    }
}

dependencies {

    // Retrofit and Moshi for networking and JSON parsing:
    implementation(libs.retrofit)
    implementation(libs.converter.moshi)

    // Google Maps Compose and Play Services
    implementation(libs.maps.compose)
    implementation(libs.play.services.maps)
    implementation(libs.play.services.location)
    implementation(libs.android.maps.utils)
    implementation(libs.maps.utils.ktx)

    // Jetpack Compose and Navigation
    implementation(libs.ui)
    implementation(libs.androidx.runtime)
    implementation(libs.material3)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.activity.compose.v140)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.material3.v110)


    // Accompanist Permissions for Compose
    implementation(libs.accompanist.permissions)

    // Kotlin Coroutines
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.play.services)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.core.v160)
    implementation(libs.androidx.ui.v140)
    implementation(libs.androidx.runtime.v140)

    // Hilt dependencies
    implementation(libs.hilt.android)
    kapt(libs.hilt.compiler)

    implementation(libs.ui.tooling.preview)
    debugImplementation(libs.ui.tooling)

    // AndroidX Core and Lifecycle dependencies
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)

    // Testing dependencies
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)


}
