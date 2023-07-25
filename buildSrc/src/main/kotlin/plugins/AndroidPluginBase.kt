package plugins

import AppInfo
import com.android.build.gradle.BaseExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.findByType
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import kotlin.reflect.KClass

abstract class AndroidPluginBase<T: BaseExtension>(
    private val clazz: KClass<T>
) : Plugin<Project> {

    abstract fun Project.projectSetup()
    abstract fun T.androidConfig(project: Project)

    override fun apply(target: Project) {
        target.projectSetup()
        target.pluginManager.apply("kotlin-android")
        target.tasks.withType<KotlinCompile> { kotlinOptions { jvmTarget = "1.8" } }
        target.configureAndroidBase()
    }

    private fun Project.configureAndroidBase() {
        extensions.findByType(clazz)?.apply {
            androidConfig(this@configureAndroidBase)
            compileSdkVersion(AppInfo.compileVersion)

            defaultConfig {
                minSdk = AppInfo.minimumSdk
                targetSdk = AppInfo.targetSdk
                versionCode = 1
                versionName = AppInfo.otakuVersionName

                testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
            }

            compileOptions {
                sourceCompatibility = JavaVersion.VERSION_1_8
                targetCompatibility = JavaVersion.VERSION_1_8
            }

            packagingOptions {
                resources {
                    excludes += "/META-INF/{AL2.0,LGPL2.1}:"
                }
            }

            dependencies {
                implementation(project(":core"))
            }
        }
    }
}