group = "space.astro"

plugins {
    base
    java

    alias(libs.plugins.jvm)
    alias(libs.plugins.spring)
    alias(libs.plugins.serialization)
}

repositories {
    maven {
        url = uri("https://maven.pkg.github.com/bot-astro/jda")
        credentials {
            this.username = property("ghpMavenUser").toString()
            this.password = property("ghpMavenPat").toString()
        }
    }
    mavenCentral()
}

// Migrate this to buildSrc or conventional builds when version catalogs are supported
subprojects {
    repositories {
        maven {
            url = uri("https://maven.pkg.github.com/bot-astro/jda")
            credentials {
                this.username = property("ghpMavenUser").toString()
                this.password = property("ghpMavenPat").toString()
            }
        }
        mavenCentral()
        maven("https://jitpack.io/")
    }

    apply(plugin = "org.jetbrains.kotlin.jvm")
    apply(plugin = "org.jetbrains.kotlin.plugin.spring")
    apply(plugin = "org.jetbrains.kotlin.plugin.serialization")

    base {
        archivesName.set("${group.toString().replace(".", "-")}-$name")
    }

    group = "${parent?.group}.${parent?.name}"

    kotlin {
        jvmToolchain {
            this.languageVersion.set(JavaLanguageVersion.of(17))
        }
    }

//    tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
//        kotlinOptions {
//            jvmTarget = "17"
//        }
//    }
}
