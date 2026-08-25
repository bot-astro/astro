version = "0.0.1"

plugins {
    id("base-conventions")
}

dependencies {
    implementation(platform(libs.mongo.bom))

    implementation(libs.bundles.logging)
    implementation(libs.bundles.db)
    implementation(libs.bundles.cache)

    implementation(libs.bigquery)
    implementation(libs.spring.core)
    implementation(libs.spring.context)
    implementation(libs.spring.boot.starter.restclient)

    implementation(libs.chargebee)
//    implementation(libs.bundles.base)
//    implementation(libs.bundles.spring.core)
//    implementation(libs.bundles.coroutines)
//    implementation(libs.bundles.web)
//    implementation(libs.bundles.serialization)
//
//    implementation(libs.nanoid)
//    implementation(libs.jda)
//    implementation(libs.chargebee)
//    implementation(libs.lettuce)
//    implementation(libs.mongo)
//    implementation(libs.bigquery)
//    implementation(kotlin("stdlib"))
}
repositories {
    mavenCentral()
}