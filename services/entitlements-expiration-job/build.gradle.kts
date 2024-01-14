plugins {
    id("com.google.cloud.tools.jib")
}

dependencies {
    implementation(libs.bundles.base)
    implementation(libs.bundles.coroutines)
    implementation(libs.bundles.web)
    implementation(libs.jda)
    implementation(project(":shared:core"))
}