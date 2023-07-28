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
include(":MangaWorldSources:tachiyomibridge:source-api")
include(":MangaWorldSources:tachiyomibridge:core_tachi")
include(":MangaWorldSources:tachiyomibridge:i18n")
include(":NovelWorldSources")
include(":AnimeWorldSources")
/*include(
    ":MangaWorldSources:tachiyomibridge",
    ":NovelWorldSources:novelupdates",
    ":MangaWorldSources:mangaread",
    ":MangaWorldSources:mangapark",
    ":MangaWorldSources:mangafourlife"
)*/

if (System.getenv("CI") == null || System.getenv("CI_MODULE_GEN") == "true") {
    // Local development (full project build)
    /*rootProject.projectDir
        .walkTopDown()
        .filter { it.isFile && it.extension == "kts" }
        .filter { it.readText().contains("id(\"otaku-source-application\")") }
        .forEach { include(":${it.parentFile?.parentFile?.name}:${it.parentFile?.name}") }*/

    rootProject.projectDir
        .walkTopDown()
        .filter { it.isFile && it.extension == "kts" }
        .filter { it.readText().contains("id(\"otaku-source-application\")") }
        .map {
            it.getTopParent(rootProject.projectDir, emptyList()).joinToString(":") { f -> f.name }
        }
        .forEach { include(":$it") }
} else {
    // Running in CI (GitHub Actions)
    val chunkSize = System.getenv("CI_CHUNK_SIZE").toInt()
    val chunk = System.getenv("CI_CHUNK_NUM").toInt()

    // Loads individual extensions
    rootProject.projectDir
        .walkTopDown()
        .filter { it.isFile && it.extension == "kts" }
        .filter { it.readText().contains("id(\"otaku-source-application\")") }
        .chunked(chunkSize)
        .toList()[chunk]
        .map {
            it.getTopParent(rootProject.projectDir, emptyList()).joinToString(":") { f -> f.name }
        }
        .forEach {
            println("Including: $it")
            include(":$it")
        }
}

tailrec fun File.getTopParent(rootDir: File, fileList: List<File>): List<File> =
    if (parentFile == null || parentFile == rootDir) {
        fileList
    } else {
        parentFile.getTopParent(rootDir, listOf(parentFile, *fileList.toTypedArray()))
    }