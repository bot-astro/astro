rootProject.name = "astro"

pluginManagement {
    includeBuild("build-logic")

    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

plugins {
    // automatically download JDK if missing
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
    // discover dependency upgrades, run `./gradlew dependencyUpdates`
    id("io.github.ben-manes.versions.settings") version "0.60.0"
}

include(
    // standalone
    "services:bot",
    "services:api-central",
//    "services:support-bot",
//    "services:entitlements-expiration-job",
    // shared
    "shared:core"
)