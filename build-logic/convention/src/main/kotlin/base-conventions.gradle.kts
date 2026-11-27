plugins {
    base
    kotlin("jvm")
    kotlin("plugin.serialization")
    kotlin("plugin.spring")
    id("io.spring.dependency-management")
    id("org.springframework.boot")
    id("org.jlleitschuh.gradle.ktlint")
    id("dev.detekt")
}

repositories {
    mavenCentral()
}

// the Spring Boot BOM pins its own coroutines version on the artifacts we don't declare (kotlinx-coroutines-core-jvm),
// mixing it with our coroutines-reactor breaks suspend controllers at runtime (NoSuchMethodError on Job.cancel)
extra["kotlin-coroutines.version"] =
    versionCatalogs
        .named("libs")
        .findVersion("coroutines")
        .get()
        .requiredVersion

sourceSets.main {
    java.srcDirs("build/generated/ksp/main/kotlin")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
}

kotlin {
    jvmToolchain(25)
}

detekt {
    buildUponDefaultConfig = true
    config.setFrom(rootProject.file("detekt.yml"))
    parallel = true
}

configurations.matching { it.name == "detekt" }.configureEach {
    resolutionStrategy.eachDependency {
        if (requested.group == "org.jetbrains.kotlin") {
            useVersion(dev.detekt.gradle.plugin.getSupportedKotlinVersion())
        }
    }
}

tasks.test {
    useJUnitPlatform()
}