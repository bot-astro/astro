plugins {
    application
    id("base-conventions")
    id("com.google.cloud.tools.jib")
    id("io.sentry.jvm.gradle")
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
        jvmFlags = listOf(
            "-XX:+PrintCommandLineFlags",
            "-XshowSettings:vm",
            "-XX:MinRAMPercentage=50.0",
            "-XX:MaxRAMPercentage=50.0"
//            "-XX:+PrintFlagsFinal",
//            "-Xlog:os+container=trace"
        )
    }
}

dependencies {
    implementation("io.netty:netty-resolver-dns-native-macos:4.2.17.Final:osx-aarch_64")
}