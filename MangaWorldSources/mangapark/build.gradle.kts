import plugins.SourceType

@Suppress("DSL_SCOPE_VIOLATION") // TODO: Remove once KTIJ-19369 is fixed
plugins {
    id("otaku-source-application")
    kotlin("plugin.serialization")
}

android {
    namespace = "com.programmersbox.mangapark"

    defaultConfig {
        versionName = "1.0.2"
    }
}

otakuSourceInformation {
    name = "MangaPark"
    classInfo = ".MangaPark"
    sourceType = SourceType.Manga
}

dependencies {
    implementation(libs.core.ktx)
    implementation(libs.appcompat)
    implementation(libs.material)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.espresso.core)
    implementation(libs.jsoup)
    implementation(libs.bundles.okHttpLibs)
    implementation(libs.koinAndroid)
    implementation(projects.mangaWorldSources)
    implementation(libs.bundles.ziplineLibs)
    implementation(libs.kotlinxSerialization)
}