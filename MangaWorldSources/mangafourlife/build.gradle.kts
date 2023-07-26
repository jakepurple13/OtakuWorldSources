import plugins.SourceType

@Suppress("DSL_SCOPE_VIOLATION") // TODO: Remove once KTIJ-19369 is fixed
plugins {
    id("otaku-source-application")
}

android {
    namespace = "com.programmersbox.mangafourlife"

    defaultConfig {
        versionName = "1.0.0"
    }
}

otakuSourceInformation {
    name = "Manga4Life"
    classInfo = ".MangaFourLife"
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
    implementation(libs.jakepurple13.gson)
    implementation(libs.kotson)
}
