import org.gradle.api.tasks.Delete

plugins {
    alias(libs.plugins.android.application) apply false
    //alias(libs.plugins.kotlin.android) apply false
    //alias(libs.plugins.kotlin.compose) apply false
    kotlin("android") version "1.8.20" apply false
}

// Register a "clean" task to delete the build directory
tasks.register("clean", Delete::class) {
    delete(rootProject.buildDir)
}
buildscript {
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        classpath(libs.hilt.android.gradle.plugin)
    }
}