plugins {
    `kotlin-dsl`
}

repositories {
    gradlePluginPortal()
}

dependencies {
    implementation(libs.kotlin.jvm.plugin)
    implementation(libs.kotlin.serialization.plugin)
    implementation(libs.kotlin.spring.plugin)
    implementation(libs.jib.plugin)
    implementation(libs.sentry.plugin)
}