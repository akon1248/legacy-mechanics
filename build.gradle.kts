import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.language.jvm.tasks.ProcessResources

val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")

plugins {
    kotlin("jvm") version libs.versions.kotlin.get()
    id("com.gradleup.shadow") version libs.versions.shadow.get()
    id("io.papermc.paperweight.userdev") version libs.versions.paperweight.userdev.get()
    id("maven-publish")
    id("extract-sources")
}

version = "1.0"
group = "com.akon"

repositories {
    mavenLocal()
    mavenCentral()
    maven("https://maven.neoforged.net/releases/")
    maven("https://repo.purpurmc.org/snapshots")
    maven("https://jitpack.io")
}

dependencies {
    compileOnly(libs.findLibrary("ignite-api").get())
    compileOnly(libs.findLibrary("fuel-loader").get())
    compileOnly(kotlin("stdlib-jdk8"))
    paperweight.devBundle(group = "org.purpurmc.purpur", version = libs.findVersion("purpur").get().requiredVersion)
    compileOnly(libs.findLibrary("sponge-mixin").get())
    compileOnly(libs.findLibrary("mixinextras-common").get())
}

kotlin {
    jvmToolchain(21)
}

tasks.named("build") {
    dependsOn(tasks.named("shadowJar"))
}

tasks.named<ProcessResources>("processResources") {
    val props = mapOf("version" to project.version)
    filesMatching(listOf("paper-plugin.yml", "ignite.mod.json")) {
        expand(props)
    }
}