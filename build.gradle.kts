group = "space.astro"

// Needed for version catalogs with gradle versions before 8.1.0
@Suppress("DSL_SCOPE_VIOLATION")
plugins {
    base
    java

    alias(libs.plugins.jvm)
    alias(libs.plugins.spring)
    alias(libs.plugins.serialization)
}

repositories {
    mavenCentral()
}

// TODO: Migrate this to buildSrc or conventional builds when version catalogs are supported
subprojects {
    repositories {
        mavenCentral()
        maven("https://m2.dv8tion.net/releases")
        maven("https://jitpack.io/")
    }

    apply(plugin = "org.jetbrains.kotlin.jvm")
    apply(plugin = "org.jetbrains.kotlin.plugin.spring")
    apply(plugin = "org.jetbrains.kotlin.plugin.serialization")

    base {
        archivesName.set("${group.toString().replace(".", "-")}-$name")
    }

    group = "${parent?.group}.${parent?.name}"
}
