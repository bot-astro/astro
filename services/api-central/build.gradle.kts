project.version = "6.0.0"

plugins {
    id("service-conventions")
}

// the AWS SDK needs the coroutines version it was built against, Spring Boot's BOM would downgrade it
// FUTURE: upgrade spring boot BOM and remove this once a new stable version gets released
extra["kotlin-coroutines.version"] = libs.versions.coroutines.get()

dependencies {
    implementation(project(":shared:core"))

    implementation(libs.bundles.service.core)
    implementation(libs.bundles.web)
    implementation(libs.bundles.serialization)
    implementation(libs.bundles.logging)
    implementation(libs.bundles.db)
    implementation(libs.bundles.cache)
    implementation(libs.s3)
    implementation(libs.bundles.bucket4j)

    implementation(libs.jda) {
        exclude(
            group = "club.minnced",
            module = "opus-java",
        )
    }
    implementation(libs.nanoid)
    implementation(libs.chargebee)
}

// api-central is small and the JVM sees every core of the node (no cpu limit), so cap what scales with core count.
// A fixed min = max heap pinned 60% of the pod memory even though live data is ~75 MB.
jib {
    container {
        jvmFlags =
            listOf(
                "-XX:+PrintCommandLineFlags",
                "-XshowSettings:vm",
                "-XX:MaxRAMPercentage=50.0",
                "-XX:MaxMetaspaceSize=192m",
                "-XX:ActiveProcessorCount=2",
            )
    }
}
