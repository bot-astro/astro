version = "1.0-SNAPSHOT"

plugins {
    id("com.google.cloud.tools.jib")
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(libs.bundles.base)
    implementation(libs.bundles.web)
    implementation(libs.bundles.discord) {
        exclude(
            group = "club.minnced",
            module = "opus-java"
        )
    }

    implementation(project(":shared:core"))
}