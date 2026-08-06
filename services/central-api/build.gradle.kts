project.version = "2.0.0"

plugins {
    id("service-conventions")
}

dependencies {
    // Generalized dependency bundles
    implementation(libs.bundles.base)
    implementation(libs.bundles.web)
    implementation(libs.bundles.serialization)
    implementation(libs.bundles.coroutines)
    implementation(libs.bundles.jwt)
    implementation(libs.bundles.caching)

    // Discord
    implementation(libs.jda)

    // Database
    implementation(libs.mongo)
    implementation(libs.nanoid)

    implementation(libs.chargebee)

    // Project
    implementation(project(":shared:core"))
}

sentry {
    // Generates a JVM (Java, Kotlin, etc.) source bundle and uploads your source code to Sentry.
    // This enables source context, allowing you to see your source
    // code as part of your stack traces in Sentry.
    includeSourceContext = true

    org = "bot-astro"
    projectName = "central-api"
    authToken = System.getenv("SENTRY_AUTH_TOKEN")
}