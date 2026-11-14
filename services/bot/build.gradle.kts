project.version = "6.0.0"

plugins {
    id("service-conventions")
}

dependencies {
    implementation(project(":shared:core"))

    implementation(libs.bundles.service.core)
    implementation(libs.bundles.web)
    implementation(libs.bundles.logging)
    implementation(libs.bundles.db)
    implementation(libs.bundles.cache)

    implementation(libs.jda) {
        exclude(
            group = "club.minnced",
            module = "opus-java",
        )
    }
    // NEVER EVER TRUST JDA KTX AND JDA IN THE SAME PROJECT UNLESS YOU ARE 100% SURE THEY USE THE SAME VERSION
    // I THINK I SPENT SOME SLEEPLESS NIGHTS ON THIS
    implementation(libs.jda.ktx) {
        exclude(
            group = "net.dv8tion",
            module = "JDA",
        )
    }

    implementation(libs.nanoid)
    implementation(libs.bigquery)
    implementation(libs.bundles.serialization)

    testImplementation(libs.junit)
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}

jib {
    container {
        jvmFlags =
            listOf(
                "-XX:+PrintCommandLineFlags",
                "-XshowSettings:vm",
                // live heap is ~1 GiB (15k guilds), the default 60% of the pod was mostly garbage.
                // 50% of the 3Gi pod limit set in infra is 1.5 GiB, the heap size tested in the canary
                "-XX:MaxRAMPercentage=50.0",
                "-XX:ActiveProcessorCount=4",
                "-XX:G1PeriodicGCInterval=120000",
                "-XX:+ExitOnOutOfMemoryError",
            )
    }
}
