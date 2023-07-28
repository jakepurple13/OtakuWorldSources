import plugins.SourceType

@Suppress("DSL_SCOPE_VIOLATION") // TODO: Remove once KTIJ-19369 is fixed
plugins {
    id("otaku-source-application")
    kotlin("plugin.serialization")
}

android {
    namespace = "com.programmersbox.aniyomibridge"

    defaultConfig {
        versionName = "1.0.0"
    }
}

otakuSourceInformation {
    name = "Aniyomi Bridge"
    classInfo = ".Aniyomi"
    sourceType = SourceType.Anime
}

dependencies {
    implementation(libs.core.ktx)
    implementation(libs.appcompat)
    implementation(libs.material)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.espresso.core)
    implementation(libs.bundles.ktor)
    implementation(libs.koinAndroid)
    api(projects.animeWorldSources.aniyomibridge.sourceApi) {
        isTransitive = true
    }
    implementation(projects.animeWorldSources.aniyomibridge.core)

}