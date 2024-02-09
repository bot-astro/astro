plugins {
    id("com.google.cloud.tools.jib")
}

dependencies {
    implementation(libs.bundles.coroutines)
    implementation(libs.bundles.caching)
    implementation(libs.bundles.web)
    implementation(project(":shared:core"))
    implementation(libs.kotlin.logging)
    implementation(libs.spring.boot.configuration.processor)
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