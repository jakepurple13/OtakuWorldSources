import plugins.SourceType

@Suppress("DSL_SCOPE_VIOLATION") // TODO: Remove once KTIJ-19369 is fixed
plugins {
    id("otaku-source-application")
    kotlin("plugin.serialization")
}

android {
    namespace = "com.programmersbox.tachiyomibridge"

    defaultConfig {
        versionName = "1.0.3"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
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
    val ktorVersion = "2.3.2"
    implementation("io.ktor:ktor-client-core:$ktorVersion")
    implementation("io.ktor:ktor-client-cio:$ktorVersion")
    implementation("io.ktor:ktor-client-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-serialization-kotlinx-json:$ktorVersion")
    implementation("io.ktor:ktor-client-logging:$ktorVersion")
    implementation(libs.koinAndroid)
}