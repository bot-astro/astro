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
                // live heap is ~1 GiB (15k guilds), the default 60% of the pod was ~3 GiB of mostly garbage, see ignored/memory-improvement-plan.md
                "-Xmx1536m",
                // no CPU limit on the pod, 4 keeps some parallelism for the startup burst (JIT, GC) without sizing everything for the node
                "-XX:ActiveProcessorCount=4",
                "-XX:G1PeriodicGCInterval=120000",
                // a heap OOM leaves the JVM half broken, better to let Kubernetes restart the pod
                "-XX:+ExitOnOutOfMemoryError",
                // TEMPORARY: confirm where the non-heap memory goes with `jcmd 1 VM.native_memory summary`, remove after
                "-XX:NativeMemoryTracking=summary",
            )
    }
}
