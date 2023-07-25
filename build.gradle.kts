// Top-level build file where you can add configuration options common to all sub-projects/modules.
@Suppress("DSL_SCOPE_VIOLATION") // TODO: Remove once KTIJ-19369 is fixed
plugins {
    kotlin("android") apply false
}
buildscript {
    dependencies {
        classpath("org.jetbrains.kotlin:kotlin-serialization:${libs.versions.kotlin.get()}")
    }
}

task("getSources") {
    doLast {
        subprojects
            .filter { it.pluginManager.hasPlugin("otaku-source-application") }
            .forEach { println("Otaku Application Source: ${it.name}") }
    }
}

true // Needed to make the Suppress annotation work for the plugins block