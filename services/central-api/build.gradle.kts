version = "1.0-SNAPSHOT"

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

    implementation(libs.chargebee)

    // Project
    implementation(project(":shared:core"))
}

plugins {
    id("com.google.cloud.tools.jib")
}

jib {
    from {
        image = "openjdk:17"
    }

    to {
        image = "ghcr.io/bot-astro/$name"
        tags = setOf(System.getenv("SEMAPHORE_GIT_SHA"), "latest")
        auth {
            username = System.getenv("GITHUB_ACTOR")
            password = System.getenv("GITHUB_TOKEN")
        }
    }
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