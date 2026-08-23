project.version = "2.0.0"

plugins {
    id("service-conventions")
}

dependencies {
    implementation(project(":shared:core"))

    implementation(libs.bundles.service.core)
    implementation(libs.bundles.web)
    implementation(libs.bundles.serialization)
    implementation(libs.bundles.logging)
    implementation(libs.bundles.db)
    implementation(libs.bundles.cache)

    implementation(libs.jda) {
        exclude(
            group = "club.minnced",
            module = "opus-java"
        )
    }
    implementation(libs.nanoid)
    implementation(libs.chargebee)

    // Generalized dependency bundles
//    implementation(libs.bundles.base)
//    implementation(libs.bundles.web)
//    implementation(libs.bundles.serialization)
//    implementation(libs.bundles.coroutines)
//    implementation(libs.bundles.jwt)
//    implementation(libs.bundles.caching)
//
//    // Discord
//    implementation(libs.jda)
//
//    // Database
//    implementation(libs.mongo)
//    implementation(libs.nanoid)
//
//    implementation(libs.chargebee)
//
//    // Project
//    implementation(project(":shared:core"))
}