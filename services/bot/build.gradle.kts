version = "1.0-SNAPSHOT"

plugins {
    id("com.google.cloud.tools.jib")
}

dependencies {
    implementation(libs.bundles.base)
    implementation(libs.bundles.web)
    implementation(libs.bundles.caching)
    implementation(libs.bundles.discord) {
        exclude(
            group = "club.minnced",
            module = "opus-java"
        )
    }

    implementation(libs.guava)
    implementation(libs.bigquery)
    implementation(libs.nanoid)
    implementation(libs.chargebee)

    implementation(project(":shared:core"))

    testImplementation(libs.junit)
    testImplementation(kotlin("test"))
}

jib {
    from {
        image = "openjdk@sha256:cf04661ba3cae4c9d788d503befc635f16ffdc740b00b0010b244d777d03adcf"
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


tasks.test {
    useJUnitPlatform()
}

sentry {
    // Generates a JVM (Java, Kotlin, etc.) source bundle and uploads your source code to Sentry.
    // This enables source context, allowing you to see your source
    // code as part of your stack traces in Sentry.
    includeSourceContext = true

    org = "bot-astro"
    projectName = "bot"
    authToken = System.getenv("SENTRY_AUTH_TOKEN")
}