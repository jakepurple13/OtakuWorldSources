package com.programmersbox.tachiyomibridge

import android.app.Application
import android.content.pm.PackageInfo
import androidx.core.content.ContextCompat
import com.programmersbox.models.ApiService
import com.programmersbox.models.ExternalApiServicesCatalog
import com.programmersbox.models.RemoteSources
import com.programmersbox.models.SourceInformation
import com.programmersbox.models.Sources
import eu.kanade.tachiyomi.network.JavaScriptEngine
import eu.kanade.tachiyomi.network.NetworkHelper
import eu.kanade.tachiyomi.network.NetworkPreferences
import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import tachiyomi.core.preference.AndroidPreferenceStore
import tachiyomi.core.preference.PreferenceStore
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.InjektModule
import uy.kohesive.injekt.api.InjektRegistrar
import uy.kohesive.injekt.api.addSingleton
import uy.kohesive.injekt.api.addSingletonFactory
import uy.kohesive.injekt.api.get

object Tachiyomi : ExternalApiServicesCatalog {

    private val loader: SourceLoader = SourceLoader()

    @OptIn(ExperimentalSerializationApi::class)
    override suspend fun initialize(app: Application) {
        println("Setup happened!")
        Injekt.importModule(
            object : InjektModule {
                override fun InjektRegistrar.registerInjectables() {
                    addSingleton(app)
                    addSingletonFactory<PreferenceStore> {
                        AndroidPreferenceStore(app)
                    }
                    addSingletonFactory {
                        NetworkPreferences(
                            preferenceStore = get(),
                            verboseLogging = false,
                        )
                    }
                    addSingletonFactory { NetworkHelper(app, get()) }
                    addSingletonFactory {
                        Json {
                            ignoreUnknownKeys = true
                            explicitNulls = false
                        }
                    }
                    addSingletonFactory { JavaScriptEngine(app) }
                }
            }
        )

        ContextCompat.getMainExecutor(app).execute { Injekt.get<NetworkHelper>() }
    }

    override val hasRemoteSources: Boolean = true

    override fun getSources(): List<SourceInformation> = listOf(
        //TODO: Gotta think about this....Not a fan of it...
        SourceInformation(
            apiService = object : ApiService {
                override val baseUrl: String get() = "https://github.com/jakepurple13/OtakuWorldSources"
                override val serviceName: String get() = "Tachiyomi Bridge"
                override val notWorking: Boolean get() = true
            },
            name = "Tachiyomi Bridge",
            icon = null,
            packageName = "com.programmersbox.tachiyomibridge",
            catalog = this
        ),
        *loader.extensionLoader
            .loadExtensions()
            .flatten()
            .map { it.copy(catalog = this) }
            .toTypedArray()
    )

    override val name: String get() = "Tachiyomi Bridge"

    override fun shouldReload(packageName: String, packageInfo: PackageInfo): Boolean {
        return packageInfo.reqFeatures?.any { it.name == "tachiyomi.extension" } == true
    }

    override suspend fun getRemoteSources(): List<RemoteSources> = Network.remoteSources().map {
        RemoteSources(
            name = it.name,
            packageName = it.pkg,
            version = it.version,
            iconUrl = "${REPO_URL_PREFIX}icon/${it.pkg}.png",
            downloadLink = "${REPO_URL_PREFIX}apk/${it.apk}",
            sources = it.sources
                ?.map { j ->
                    Sources(
                        name = "${j.name} - ${j.lang}",
                        baseUrl = j.baseUrl,
                        version = it.version
                    )
                }
                .orEmpty()
        )
    }
}

private object Network {
    private val json = Json {
        isLenient = true
        prettyPrint = true
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    private val client by lazy {
        HttpClient {
            install(ContentNegotiation) { json(json) }
        }
    }

    suspend fun remoteSources() = client.get("${REPO_URL_PREFIX}index.min.json")
        .bodyAsText()
        .let { json.decodeFromString<List<ExtensionJsonObject>>(it) }
}

private fun ExtensionJsonObject.extractLibVersion(): Double {
    return version.substringBeforeLast('.').toDouble()
}

private const val REPO_URL_PREFIX =
    "https://raw.githubusercontent.com/tachiyomiorg/extensions/repo/"

@Serializable
private data class ExtensionJsonObject(
    val name: String,
    val pkg: String,
    val apk: String,
    val lang: String,
    val code: Long,
    val version: String,
    val nsfw: Int,
    val hasReadme: Int = 0,
    val hasChangelog: Int = 0,
    val sources: List<ExtensionSourceJsonObject>?,
)

@Serializable
private data class ExtensionSourceJsonObject(
    val id: Long,
    val lang: String,
    val name: String,
    val baseUrl: String,
)