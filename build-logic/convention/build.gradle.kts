plugins {
    `kotlin-dsl`
}

repositories {
    gradlePluginPortal()
}

dependencies {
    compileOnly(libs.kotlin.jvm.plugin)
    compileOnly(libs.kotlin.serialization.plugin)
}