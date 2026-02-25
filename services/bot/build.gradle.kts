version = "1.0-SNAPSHOT"

plugins {
    id("com.google.cloud.tools.jib")
}

dependencies {
    implementation(libs.bundles.base)
    implementation(libs.bundles.web)
    implementation(libs.bundles.coroutines)
    implementation(libs.bundles.caching)
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
    implementation(libs.bigquery)
    implementation(libs.nanoid)
    implementation(libs.chargebee)

    implementation(project(":shared:core"))

    testImplementation(libs.junit)
    testImplementation(kotlin("test"))
}

jib {
    from {
        image = "amazoncorretto:21-al2023-headless"
    }

    to {
        image = "ghcr.io/${project.property("ghcrOrg")}/$name"
        tags = setOf(System.getenv("SEMAPHORE_GIT_SHA"), "latest")
        auth {
            username = System.getenv("GITHUB_ACTOR")
            password = System.getenv("GITHUB_TOKEN")
        }
    }

    container {
        jvmFlags = listOf(
            "-XX:+PrintCommandLineFlags",
            "-XshowSettings:vm",
//            "-XX:+PrintFlagsFinal",
//            "-Xlog:os+container=trace"
        )
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