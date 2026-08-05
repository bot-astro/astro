version = "0.0.1"

plugins {
    id("base-conventions")
}

dependencies {
    implementation(libs.bundles.base)
    implementation(libs.bundles.spring.core)
    implementation(libs.bundles.coroutines)
    implementation(libs.bundles.web)
    implementation(libs.bundles.serialization)
    implementation(libs.bundles.ktor.client)

    implementation(libs.nanoid)
    implementation(libs.jda)
    implementation(libs.chargebee)
    implementation(libs.lettuce)
    implementation(libs.mongo)
    implementation(libs.kmongo)
    implementation(libs.bigquery)
    implementation(kotlin("stdlib"))
}
repositories {
    mavenCentral()
}