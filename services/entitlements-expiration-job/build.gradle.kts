plugins {
    id("com.google.cloud.tools.jib")
}

dependencies {
    implementation(libs.bundles.base)
    implementation(libs.bundles.coroutines)
    implementation(libs.bundles.web)
    implementation(libs.jda)
    implementation(project(":shared:core"))
}

jib {
    from {
        image = "amazoncorretto@sha256:ffe99c76c9304663a7adc8a292c186215f78fc918bfd0b3b0a4b57b3c0d90fd1"
    }

    to {
        image = "ghcr.io/${project.property("ghcrOrg")}/$name"
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
    projectName = "entitlements-expiration-job"
    authToken = System.getenv("SENTRY_AUTH_TOKEN")
}