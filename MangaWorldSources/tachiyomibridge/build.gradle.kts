import plugins.SourceType

@Suppress("DSL_SCOPE_VIOLATION") // TODO: Remove once KTIJ-19369 is fixed
plugins {
    id("otaku-source-application")
    kotlin("plugin.serialization")
}

android {
    namespace = "com.programmersbox.tachiyomibridge"

    defaultConfig {
        versionName = "1.0.7"
    }
}

otakuSourceInformation {
    name = "Tachiyomi Bridge"
    classInfo = ".Tachiyomi"
    sourceType = SourceType.Manga
}

dependencies {
    api(projects.mangaWorldSources.tachiyomibridge.sourceApi) {
        isTransitive = true
    }
    implementation(libs.core.ktx)
    implementation(libs.appcompat)
    implementation(libs.material)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.espresso.core)
    implementation(projects.mangaWorldSources.tachiyomibridge.coreTachi)
    implementation(libs.bundles.ktor)
    implementation(libs.koinAndroid)
}