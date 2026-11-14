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
        // glibc creates up to 8 malloc arenas per detected core, which fragments native memory and bloats the RSS.
        // 2 is the commonly recommended value for containerized JVMs.
        // Sentry automatically reads SENTRY_RELEASE and it's the easiest way to provide the correct release
        environment =
            mapOf("MALLOC_ARENA_MAX" to "2") +
                (System.getenv("SEMAPHORE_GIT_SHA")?.let { mapOf("SENTRY_RELEASE" to it) } ?: emptyMap())

        jvmFlags = listOf(
            "-XX:+PrintCommandLineFlags",
            "-XshowSettings:vm",
            "-XX:MaxRAMPercentage=60.0",
            // Pods have no CPU limit on purpose (startup needs bursts), so the JVM would see every core of the node
            // and size GC, JIT and Netty threads (and malloc arenas) for it. Services with different needs override jvmFlags.
            "-XX:ActiveProcessorCount=2",
            // return unused heap to the OS when idle, G1 otherwise never shrinks
            "-XX:G1PeriodicGCInterval=120000"
//            "-XX:+PrintFlagsFinal",
//            "-Xlog:os+container=trace"
        )
    }
}

dependencies {
    implementation("io.netty:netty-resolver-dns-native-macos:4.2.17.Final:osx-aarch_64")
}