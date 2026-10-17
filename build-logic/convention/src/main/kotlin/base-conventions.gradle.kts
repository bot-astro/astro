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
    parallel = true
}

tasks.test {
    useJUnitPlatform()
}