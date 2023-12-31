repositories {
    google()
    mavenCentral()
    gradlePluginPortal()
}

plugins {
    `java-gradle-plugin`
    `kotlin-dsl`
    `version-catalog`
}

gradlePlugin {
    plugins {
        register("otaku-source-application") {
            id = "otaku-source-application"
            implementationClass = "plugins.AndroidSourcePlugin"
        }
    }
}

dependencies {
    implementation(gradleApi())
    implementation(libs.gradle)
    implementation(libs.kotlinStLib)
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:${libs.versions.kotlin.get()}")
}