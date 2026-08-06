project.version = "2.0.0"

plugins {
    id("service-conventions")
}

dependencies {
    implementation(libs.bundles.base)
    implementation(libs.bundles.coroutines)
    implementation(libs.bundles.web)
    implementation(libs.jda)
    implementation(project(":shared:core"))
}

sentry {
    // Generates a JVM (Java, Kotlin, etc.) source bundle and uploads your source code to Sentry.
    // This enables source context, allowing you to see your source
    // code as part of your stack traces in Sentry.
    includeSourceContext = true

    org = "bot-astro"
    projectName = "entitlements-expiration-job"
    authToken = System.getenv("SENTRY_AUTH_TOKEN")
}