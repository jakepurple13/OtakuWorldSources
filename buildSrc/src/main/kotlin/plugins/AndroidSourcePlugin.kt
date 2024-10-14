package plugins

import com.android.build.api.variant.AndroidComponentsExtension
import com.android.build.gradle.internal.dsl.BaseAppModuleExtension
import org.gradle.api.Project
import org.gradle.kotlin.dsl.findByType
import javax.inject.Inject
import kotlin.properties.Delegates

class AndroidSourcePlugin :
    AndroidPluginBase<BaseAppModuleExtension>(BaseAppModuleExtension::class) {

    override fun apply(target: Project) {
        super.apply(target)

        target.extensions.create(
            "otakuSourceInformation",
            SourceInformation::class.java,
            target
        )
    }

    override fun Project.projectSetup() {
        pluginManager.apply("com.android.application")
    }

    override fun BaseAppModuleExtension.androidConfig(project: Project) {
        signingConfigs {
            create("release").apply {
                storeFile = project.rootProject.file("signingkey.jks")
                storePassword = System.getenv("KEY_STORE_PASSWORD")
                keyAlias = System.getenv("ALIAS")
                keyPassword = System.getenv("KEY_PASSWORD")
            }
        }
        buildTypes {
            release {
                signingConfig = signingConfigs.getByName("release")
                isMinifyEnabled = false
                proguardFiles(
                    getDefaultProguardFile("proguard-android-optimize.txt"),
                    "proguard-rules.pro"
                )
            }
        }

        @Suppress("UnstableApiUsage")
        project.extensions.findByType(AndroidComponentsExtension::class)?.finalizeDsl {
            val version = defaultConfig.versionName
            val type = it.defaultConfig.manifestPlaceholders["extSuffix"]
            val name = it.defaultConfig.manifestPlaceholders["extName"]
                .toString()
                .lowercase()
            project.setProperty(
                "archivesBaseName",
                "${type}world-$name-v$version".replace(" ", "-")
            )
            //project.archivesName.set("${type}world-$name-v$version".replace(" ", "-"))
        }
    }
}

abstract class SourceInformation @Inject constructor(private val project: Project) {
    private val source = SourceInfo()

    var name: String
        get() = source.metadataName
        set(value) {
            source.metadataName = value
            setInfo("extName", value)
        }

    var classInfo: String
        get() = source.metadataClass
        set(value) {
            source.metadataClass = value
            setInfo("extClass", value)
        }

    var sourceType: SourceType
        get() = SourceType.Nothing
        set(value) {
            source.sourceType = value
            setInfo(
                "extSuffix",
                value
                    .takeIf { it != SourceType.Nothing }
                    ?.name
                    ?.lowercase() ?: error("SourceType not Set")
            )
        }

    private fun setInfo(key: String, value: String) {
        project.extensions
            .findByType(BaseAppModuleExtension::class.java)
            ?.apply { defaultConfig { manifestPlaceholders[key] = value } }
    }
}

internal class SourceInfo {
    var metadataName by Delegates.notNull<String>()
    var metadataClass by Delegates.notNull<String>()
    var sourceType by Delegates.notNull<SourceType>()
}

enum class SourceType { Anime, Manga, Novel, Nothing }