pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        google()
    }
    
    plugins {
        kotlin("jvm") version "2.0.21"
    }
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
        google()
    }
}

rootProject.name = "steam-utils"
