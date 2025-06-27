import plugins.SourceType

@Suppress("DSL_SCOPE_VIOLATION") // TODO: Remove once KTIJ-19369 is fixed
plugins {
    id("otaku-source-application")
}

android {
    namespace = "com.programmersbox.novelupdates"
    defaultConfig {
        versionName = "1.0.1"
    }
}

otakuSourceInformation {
    name = "NovelUpdates"
    classInfo = ".NovelUpdates"
    sourceType = SourceType.Novel
}

dependencies {
    implementation(libs.core.ktx)
    implementation(libs.appcompat)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.espresso.core)
    implementation(libs.jsoup)
    implementation(libs.bundles.ktor)
    implementation("io.ktor:ktor-client-okhttp:${libs.versions.ktorVersion.get()}")
    implementation(libs.koinAndroid)
    implementation("com.tfowl.ktor:ktor-jsoup:2.3.0")
}