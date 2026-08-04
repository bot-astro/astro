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
}

include(
    // standalone
//    "services:bot",
//    "services:central-api",
//    "services:support-bot",
//    "services:entitlements-expiration-job",
    // shared
//    "shared:core"
)