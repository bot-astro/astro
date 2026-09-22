version = "0.0.1"

plugins {
    id("base-conventions")
}

dependencies {
    implementation(platform(libs.mongo.bom))

    implementation(libs.bundles.logging)
    implementation(libs.bundles.db)
    implementation(libs.bundles.cache)

    implementation(libs.jackson.kotlin)
    implementation(libs.bigquery)
    implementation(libs.spring.core)
    implementation(libs.spring.context)
    implementation(libs.spring.boot.starter.restclient)
    implementation(libs.jda)
    implementation(libs.nanoid)
    implementation(libs.bundles.icu4j)

    implementation(libs.chargebee)
//    implementation(libs.bundles.base)
//    implementation(libs.bundles.spring.core)
//    implementation(libs.bundles.coroutines)
//    implementation(libs.bundles.web)
//    implementation(libs.bundles.serialization)
//
//    implementation(libs.nanoid)
//    implementation(libs.jda)
//    implementation(libs.chargebee)
//    implementation(libs.lettuce)
//    implementation(libs.mongo)
//    implementation(libs.bigquery)
//    implementation(kotlin("stdlib"))
}
repositories {
    mavenCentral()
}

// Localization
val generateLocalesIndex = tasks.register("generate_locales_index") {
    description = "Reads the list of i18n files and generates an index with the list of locales available"

    val i18nFiles = fileTree("src/main/resources/i18n") { include("*.json") }
    val outDir = layout.buildDirectory.dir("generated/locale-index")
    inputs.files(i18nFiles)
    outputs.dir(outDir)

    doLast {
        val tags = i18nFiles.files.map { it.nameWithoutExtension }.sorted()
        val target = outDir.get().asFile.resolve("i18n").apply { mkdirs() }
        target.resolve("locales.txt").writeText(tags.joinToString("\n"))
    }
}

tasks.processResources {
    from(generateLocalesIndex)
}
