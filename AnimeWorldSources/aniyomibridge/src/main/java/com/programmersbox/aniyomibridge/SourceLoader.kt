package com.programmersbox.aniyomibridge

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import android.text.format.DateFormat
import com.programmersbox.models.ApiService
import com.programmersbox.models.ChapterModel
import com.programmersbox.models.InfoModel
import com.programmersbox.models.ItemModel
import com.programmersbox.models.SourceInformation
import com.programmersbox.models.Storage
import dalvik.system.PathClassLoader
import eu.kanade.tachiyomi.animesource.AnimeSource
import eu.kanade.tachiyomi.animesource.AnimeSourceFactory
import eu.kanade.tachiyomi.animesource.model.SAnime
import eu.kanade.tachiyomi.animesource.model.SEpisode
import eu.kanade.tachiyomi.animesource.online.AnimeHttpSource
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.text.SimpleDateFormat
import java.util.Locale

class SourceLoader : KoinComponent {
    private val context: Context by inject()

    private val systemDateTimeFormat = SimpleDateFormat(
        "${(DateFormat.getDateFormat(context) as SimpleDateFormat).toLocalizedPattern()} ${(DateFormat.getTimeFormat(context) as SimpleDateFormat).toLocalizedPattern()}",
        Locale.getDefault()
    )

    val extensionLoader = ExtensionLoader2<Any, List<SourceInformation>>(
        context,
        "tachiyomi.animeextension",
        "tachiyomi.animeextension.class",
    ) { t, a, p ->
        when (t) {
            is AnimeSource -> listOf(t)
            is AnimeSourceFactory -> t.createSources()
            else -> emptyList()
        }
            .filterIsInstance<AnimeHttpSource>()
            .map { toSource(it, a, p) }
    }

    val toSource: (AnimeHttpSource, ApplicationInfo, PackageInfo) -> SourceInformation = { t, a, p ->
        SourceInformation(
            apiService = object : ApiService {
                override val baseUrl: String get() = t.baseUrl

                override val canScroll: Boolean = true

                override val serviceName: String get() = t.name + t.lang

                private fun MutableMap<String, Any>.storeSAnime(sAnime: SAnime) {
                    set("sanime_title", sAnime.title)
                    set("sanime_url", sAnime.url)
                    set("sanime_artist", sAnime.artist.orEmpty())
                    set("sanime_author", sAnime.author.orEmpty())
                    set("sanime_thumbnail_url", sAnime.thumbnail_url.orEmpty())
                    set("sanime_description", sAnime.description.orEmpty())
                    set("sanime_genre", sAnime.genre.orEmpty())
                    set("sanime_initialized", sAnime.initialized)
                    set("sanime_status", sAnime.status)
                }

                private fun MutableMap<String, Any>.retrieveSMana() = SAnime.create().apply {
                    title = getOrDefault("sanime_title", "title").toString()
                    url = get("sanime_url").toString()
                    artist = get("sanime_artist").toString()
                    author = get("sanime_author").toString()
                    thumbnail_url = get("sanime_thumbnail_url").toString()
                    description = get("sanime_description").toString()
                    genre = get("sanime_genre").toString()
                    initialized = getOrDefault("sanime_initialized", false) as Boolean
                    status = (getOrDefault("sanime_status", 1) as Number).toInt()
                }

                override suspend fun recent(page: Int): List<ItemModel> {
                    return t.fetchLatestUpdates(page).toBlocking().first().animes.map {
                        ItemModel(
                            title = it.title,
                            description = it.description.orEmpty(),
                            url = it.url,
                            imageUrl = it.thumbnail_url.orEmpty(),
                            source = this
                        ).also { item -> item.otherExtras.storeSAnime(it) }
                    }
                }

                override suspend fun allList(page: Int): List<ItemModel> {
                    return t.fetchPopularAnime(page).toBlocking().first().animes.map {
                        ItemModel(
                            title = it.title,
                            description = it.description.orEmpty(),
                            url = it.url,
                            imageUrl = it.thumbnail_url.orEmpty(),
                            source = this
                        ).also { item -> item.otherExtras.storeSAnime(it) }
                    }
                }

                override suspend fun search(
                    searchText: CharSequence,
                    page: Int,
                    list: List<ItemModel>
                ): List<ItemModel> {
                    return t.fetchSearchAnime(page, searchText.toString(), t.getFilterList())
                        .toBlocking()
                        .first()
                        .animes
                        .map {
                            ItemModel(
                                title = it.title,
                                description = it.description.orEmpty(),
                                url = it.url,
                                imageUrl = it.thumbnail_url.orEmpty(),
                                source = this
                            ).also { item -> item.otherExtras.storeSAnime(it) }
                        }
                }

                private fun MutableMap<String, Any>.storeSEpisode(sEpisode: SEpisode) {
                    set("sepisode_url", sEpisode.url)
                    set("sepisode_name", sEpisode.name)
                    set("sepisode_episode_number", sEpisode.episode_number)
                    set("sepisode_date_upload", sEpisode.date_upload)
                }

                private fun MutableMap<String, Any>.retrieveSEpisode() = SEpisode.create().apply {
                    url = getOrDefault("sepisode_url", "").toString()
                    name = getOrDefault("sepisode_name", "").toString()
                    episode_number = getOrDefault("sepisode_episode_number", 1f) as Float
                    date_upload = getOrDefault("sepisode_date_upload", 0L) as Long
                }

                override suspend fun itemInfo(model: ItemModel): InfoModel {
                    val f = model.otherExtras.retrieveSMana().apply {
                        if (url.isBlank() || url == "null") url = model.url
                        if (title.isBlank() || title == "null") title = model.title
                        if (description.isNullOrBlank() || title == "null") description =
                            model.description
                        if (thumbnail_url.isNullOrBlank()) thumbnail_url = model.imageUrl
                    }
                    val s = t.getAnimeDetails(f)
                    return InfoModel(
                        title = model.title,
                        description = s.description.orEmpty(),
                        url = model.url,
                        imageUrl = model.imageUrl,
                        chapters = t.fetchEpisodeList(f)
                            .toBlocking()
                            .first()
                            .map {
                                ChapterModel(
                                    name = it.name,
                                    url = it.url,
                                    uploaded = runCatching { systemDateTimeFormat.format(it.date_upload) }
                                        .fold(
                                            onSuccess = { d -> d },
                                            onFailure = { _ -> it.date_upload.toString() }
                                        ),
                                    sourceUrl = model.url,
                                    source = this
                                ).also { item -> item.otherExtras.storeSEpisode(it) }
                            },
                        genres = s.genre?.split(",").orEmpty(),
                        alternativeNames = emptyList(),
                        source = this
                    )
                }

                override suspend fun chapterInfo(chapterModel: ChapterModel): List<Storage> {
                    val s = chapterModel.otherExtras.retrieveSEpisode()
                    return t.fetchVideoList(s)
                        .toBlocking()
                        .first()
                        .map {
                            Storage(
                                link = it.videoUrl.orEmpty(),
                                source = chapterModel.url,
                                quality = it.quality,
                                sub = "Yes"
                            ).apply { it.headers?.forEach { headers[it.first] = it.second } }
                        }
                }
            },
            name = t.name,
            icon = context.packageManager.getApplicationIcon(p.packageName),
            packageName = p.packageName
        )
    }
}

private val PACKAGE_FLAGS =
    PackageManager.GET_CONFIGURATIONS or if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        PackageManager.GET_SIGNING_CERTIFICATES
    } else {
        @Suppress("DEPRECATION")
        PackageManager.GET_SIGNATURES
    }

class ExtensionLoader2<T, R>(
    private val context: Context,
    private val extensionFeature: String,
    private val metadataClass: String,
    private val mapping: (T, ApplicationInfo, PackageInfo) -> R
) : KoinComponent {
    @SuppressLint("QueryPermissionsNeeded")
    fun loadExtensions(mapped: (T, ApplicationInfo, PackageInfo) -> R = mapping): List<R> {
        val packageManager = context.packageManager
        val packages = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.getInstalledPackages(PackageManager.PackageInfoFlags.of(PACKAGE_FLAGS.toLong()))
        } else {
            packageManager.getInstalledPackages(PACKAGE_FLAGS)
        }
            .filter { it.reqFeatures.orEmpty().any { f -> f.name == extensionFeature } }

        return runBlocking {
            packages
                .map { async { loadExtension(it, mapped) } }
                .flatMap { it.await() }
        }
    }

    private fun loadExtension(
        packageInfo: PackageInfo,
        mapped: (T, ApplicationInfo, PackageInfo) -> R
    ): List<R> {
        val appInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.packageManager.getApplicationInfo(
                packageInfo.packageName,
                PackageManager.ApplicationInfoFlags.of(PackageManager.GET_META_DATA.toLong())
            )
        } else {
            context.packageManager.getApplicationInfo(
                packageInfo.packageName,
                PackageManager.GET_META_DATA
            )
        }

        val classLoader = PathClassLoader(
            appInfo.sourceDir,
            null,
            this::class.java.classLoader //THIS!!! THIS GOT IT TO WORK! WOOOOO!
        )

        return appInfo.metaData.getString(metadataClass)
            .orEmpty()
            .split(";")
            .map {
                val sourceClass = it.trim()
                if (sourceClass.startsWith(".")) {
                    packageInfo.packageName + sourceClass
                } else {
                    sourceClass
                }
            }
            .mapNotNull {
                runCatching {
                    @Suppress("UNCHECKED_CAST")
                    Class.forName(it, false, classLoader)
                        .getDeclaredConstructor()
                        .newInstance() as? T
                }
                    .onFailure { it.printStackTrace() }
                    .getOrNull()
            }
            .mapNotNull {
                runCatching { mapped(it, appInfo, packageInfo) }
                    .onFailure { it.printStackTrace() }
                    .getOrNull()
            }
    }
}