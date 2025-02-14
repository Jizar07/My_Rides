pluginManagement {
    repositories {
        google()  // Ensure Google repository is included
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()  // Ensure the Google repository is included
        mavenCentral()
    }
}

rootProject.name = "My Rides"
include(":app")
