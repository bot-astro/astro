version = "1.0-SNAPSHOT"

plugins {
    base
    java
}

dependencies {
    implementation(variantOf(libs.dnsMacOS) { classifier("osx-aarch_64")})
    implementation(libs.bundles.base)
    implementation(libs.bundles.spring.core)
    implementation(libs.bundles.coroutines)
    implementation(libs.bundles.web)
    implementation(libs.bundles.serialization)

    implementation(libs.nanoid)
    implementation(libs.jda)
    implementation(libs.chargebee)
    implementation(libs.lettuce)
    implementation(libs.mongo)
    implementation(libs.influx)
    implementation(libs.bigquery)
}
