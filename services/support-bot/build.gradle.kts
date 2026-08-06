project.version = "2.0.0"

plugins {
    id("service-conventions")
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
    implementation(libs.chargebee)
    implementation(project(":shared:core"))
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