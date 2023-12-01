version = "1.0-SNAPSHOT"

plugins {
    base
    java
}

dependencies {
    implementation(libs.bundles.base)
    implementation(libs.bundles.web)
    implementation(libs.bundles.serialization)

    implementation(libs.nanoid)
    implementation(libs.jda)
    implementation(libs.lettuce)
    implementation(libs.mongo)
    implementation(libs.bigquery)
}
