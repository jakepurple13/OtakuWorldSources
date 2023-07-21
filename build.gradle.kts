// Top-level build file where you can add configuration options common to all sub-projects/modules.
@Suppress("DSL_SCOPE_VIOLATION") // TODO: Remove once KTIJ-19369 is fixed
plugins {
    alias(libs.plugins.kotlinAndroid) apply false
}
buildscript {
    dependencies {
        classpath("org.jetbrains.kotlin:kotlin-serialization:${libs.versions.kotlin.get()}")
    }
}

task("getSources") {
    subprojects
        .filter {

            it.pluginManager.hasPlugin("otaku-source-application")
            /*it.afterEvaluate {
                it.plugins.hasPlugin("otaku-source-application")
            }*/
        }
        .forEach { println(it.name) }
}

true // Needed to make the Suppress annotation work for the plugins block