// settings.gradle.kts - This file defines which modules are part of your project.
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal() // For plugins available on the Gradle Plugin Portal
    }
}
dependencyResolutionManagement {
    // This mode ensures that dependencies are resolved only from the repositories defined here.
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "KenyaTourism" // Your project name
include(":app") // Include your main application module here