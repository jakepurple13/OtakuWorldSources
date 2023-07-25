enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
        maven("https://jitpack.io")
    }
}
dependencyResolutionManagement {
    versionCatalogs {
        create("kotlinx") {
            from(files("gradle/kotlinx.versions.toml"))
        }
    }
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven("https://jitpack.io")
    }
}

rootProject.name = "OtakuWorldSources"
include(":app")
include(":MangaWorldSources")
include(":core")
include(":MangaWorldSources:tachiyomibridge")
include(":MangaWorldSources:tachiyomibridge:source-api")
include(":MangaWorldSources:tachiyomibridge:core_tachi")
include(":MangaWorldSources:tachiyomibridge:i18n")
include(":NovelWorldSources")
include(":NovelWorldSources:novelupdates")
