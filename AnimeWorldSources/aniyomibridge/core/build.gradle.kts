plugins {
    id("com.android.library")
    kotlin("android")
    kotlin("plugin.serialization")
}

android {
    namespace = "eu.kanade.tachiyomi.core"
    compileSdk = 33

    defaultConfig {
        minSdk = 24

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
        freeCompilerArgs += listOf(
            "-Xcontext-receivers",
            "-opt-in=kotlinx.serialization.ExperimentalSerializationApi",
        )
    }
}

dependencies {
    api(libs.logcat)

    api(libs.rxjava)

    api(libs.okhttp.core)
    api(libs.okhttp.logging)
    api(libs.okhttp.dnsoverhttps)
    api(libs.okio)

    implementation(libs.image.decoder)

    implementation(libs.unifile)

    api(kotlinx.coroutines.core)
    api(kotlinx.serialization.json)
    api(kotlinx.serialization.json.okio)

    api(libs.preferencektx)

    implementation(libs.jsoup)

    // Sort
    implementation(libs.natural.comparator)

    // JavaScript engine
    implementation(libs.bundles.js.engine)

    // FFmpeg-kit
    //implementation(libs.ffmpeg.kit)
}
