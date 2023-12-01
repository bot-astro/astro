version = "1.0-SNAPSHOT"

plugins {
    id("com.google.cloud.tools.jib")
}

dependencies {
    implementation(libs.bundles.base)
    implementation(libs.bundles.web)
    implementation(libs.bundles.caching)
    implementation(libs.bundles.discord) {
        exclude(
            group = "club.minnced",
            module = "opus-java"
        )
    }

    implementation(libs.guava)

    implementation(libs.bigquery)

    implementation(project(":shared:core"))

    testImplementation(libs.junit)
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}