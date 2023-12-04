plugins {
    id("com.google.cloud.tools.jib")
}

dependencies {
    implementation(libs.bundles.base)
    implementation(libs.bundles.coroutines)
    implementation(libs.bundles.web)
    implementation(project(":shared:core"))
}