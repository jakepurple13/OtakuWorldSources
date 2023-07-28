import plugins.SourceType

@Suppress("DSL_SCOPE_VIOLATION") // TODO: Remove once KTIJ-19369 is fixed
plugins {
    id("otaku-source-application")
}

android {
    namespace = "com.programmersbox.kawaiifu"
    defaultConfig {
        versionName = "1.0.0"
    }
}

otakuSourceInformation {
    name = "Kawaiifu"
    classInfo = ".Kawaiifu"
    sourceType = SourceType.Anime
}

dependencies {
    implementation(libs.core.ktx)
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.jsoup)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.espresso.core)
    implementation(projects.animeWorldSources)
}