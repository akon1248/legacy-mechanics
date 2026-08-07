pluginManagement {
    repositories {
        gradlePluginPortal()
        maven("https://repo.papermc.io/repository/maven-public/")
    }
}

plugins {
    // Not sourced from gradle/libs.versions.toml: the settings file's own plugins {} block
    // resolves before dependencyResolutionManagement's version catalog exists, so catalog
    // accessors aren't available here.
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "legacy-mechanics"
includeBuild("build-logic")
