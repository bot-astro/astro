plugins {
    application
    id("base-conventions")
    id("com.google.cloud.tools.jib")
    id("io.sentry.jvm.gradle")
}

sentry {
    // Uploads a source bundle so stack traces show source context.
    // The Sentry project slug matches the Gradle project (service) name, except api-central (Sentry project is central-api).
    includeSourceContext = true

    org = "bot-astro"
    projectName = if (project.name == "api-central") "central-api" else project.name
    authToken = System.getenv("SENTRY_AUTH_TOKEN")
}

jib {
    from {
        image = "amazoncorretto:25-al2023-headless"
    }

    to {
        image = "ghcr.io/${System.getenv("GHCR_ORGANIZATION")}/${project.name}"
        tags = setOf(System.getenv("SEMAPHORE_GIT_SHA"), project.version.toString(), "latest")
        auth {
            username = System.getenv("GITHUB_ACTOR")
            password = System.getenv("GITHUB_TOKEN")
        }
    }

    container {
        environment = buildMap {
            put("MALLOC_ARENA_MAX", "2")

            // Sentry automatically reads this env variable and it's the easiest way to provide the correct release
            System.getenv("SEMAPHORE_GIT_SHA")?.let { put("SENTRY_RELEASE", it) }
        }

        jvmFlags = listOf(
            "-XX:+PrintCommandLineFlags",
            "-XshowSettings:vm",
            "-XX:MinRAMPercentage=60.0",
            "-XX:MaxRAMPercentage=60.0",
            "-XX:ActiveProcessorCount=2",
//            "-XX:+PrintFlagsFinal",
//            "-Xlog:os+container=trace"
        )
    }
}

dependencies {
    implementation("io.netty:netty-resolver-dns-native-macos:4.2.17.Final:osx-aarch_64")
}