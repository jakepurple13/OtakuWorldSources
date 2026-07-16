plugins {
    kotlin("multiplatform")
    id("kotlinx-serialization")
    id("com.android.library")
}

kotlin {
    android {
        compilations.all {
            kotlinOptions { jvmTarget = "1.8" }
        }
    }
    sourceSets {
        val commonMain by getting {
            dependencies {
                api(libs.kotlinxSerialization)
                api(libs.injekt.core)
                api("io.reactivex:rxjava:1.3.8")
                api(libs.jsoup)
            }
        }
        val androidMain by getting {
            dependencies {
                implementation(projects.mangaWorldSources.tachiyomibridge.coreTachi)
                api("androidx.preference:preference-ktx:1.2.1")

                // Workaround for https://youtrack.jetbrains.com/issue/KT-57605
                implementation(kotlinx.coroutines.android)
                implementation(project.dependencies.platform(kotlinx.coroutines.bom))
            }
        }
    }
}

android {
    namespace = "eu.kanade.tachiyomi.source"
    compileSdk = 33

    defaultConfig {
        minSdk = 24

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}
