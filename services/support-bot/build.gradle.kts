plugins {
    id("com.google.cloud.tools.jib")
}

dependencies {
    implementation(libs.bundles.base)
    implementation(libs.bundles.caching)
    implementation(libs.bundles.coroutines)
    implementation(libs.bundles.web)
    implementation(libs.jda) {
        exclude(
            group = "club.minnced",
            module = "opus-java"
        )
    }
    // NEVER EVER TRUST JDA KTX AND JDA IN THE SAME PROJECT
    implementation(libs.jda.ktx) {
        exclude(
            group = "net.dv8tion",
            module = "JDA"
        )
    }
    implementation(libs.guava)
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
    projectName = "support-bot"
    authToken = System.getenv("SENTRY_AUTH_TOKEN")
}