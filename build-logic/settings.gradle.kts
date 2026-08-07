dependencyResolutionManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
    // build-logic is a separate included build with its own settings file, so it doesn't
    // automatically see the root project's gradle/libs.versions.toml -- point it at the same
    // file explicitly so precompiled script plugins here and the main build share one catalog.
    versionCatalogs {
        create("libs") {
            from(files("../gradle/libs.versions.toml"))
        }
    }
}
rootProject.name = "build-logic"
