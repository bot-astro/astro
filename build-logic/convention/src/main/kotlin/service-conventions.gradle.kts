plugins {
    application
    id("base-conventions")
    id("com.google.cloud.tools.jib")
    id("io.sentry.jvm.gradle")
}

sentry {
    // Uploads a source bundle so stack traces show source context.
    // The Sentry project slug must match the Gradle project (service) name.
    includeSourceContext = true

    org = "bot-astro"
    projectName = project.name
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
        // Sentry automatically reads this env variable
        // and it's the easiest way to provide the correct release
        System.getenv("SEMAPHORE_GIT_SHA")?.let {
            environment = mapOf("SENTRY_RELEASE" to it)
        }

        jvmFlags = listOf(
            "-XX:+PrintCommandLineFlags",
            "-XshowSettings:vm",
            "-XX:MinRAMPercentage=60.0",
            "-XX:MaxRAMPercentage=60.0"
//            "-XX:+PrintFlagsFinal",
//            "-Xlog:os+container=trace"
        )
    }
}

dependencies {
    implementation("io.netty:netty-resolver-dns-native-macos:4.2.17.Final:osx-aarch_64")
}