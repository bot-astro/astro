project.version = "2.0.0"

plugins {
    id("service-conventions")
}

dependencies {
    implementation(project(":shared:core"))

    implementation(libs.bundles.service.core)
    implementation(libs.bundles.logging)
    implementation(libs.bundles.db)
    implementation(libs.bundles.cache)

    implementation(libs.jda) {
        exclude(
            group = "club.minnced",
            module = "opus-java"
        )
    }
    // NEVER EVER TRUST JDA KTX AND JDA IN THE SAME PROJECT UNLESS YOU ARE 100% SURE THEY USE THE SAME VERSION
    // I THINK I SPENT SOME SLEEPLESS NIGHTS ON THIS
    implementation(libs.jda.ktx) {
        exclude(
            group = "net.dv8tion",
            module = "JDA"
        )
    }

    implementation(libs.nanoid)

    testImplementation(libs.junit)
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}