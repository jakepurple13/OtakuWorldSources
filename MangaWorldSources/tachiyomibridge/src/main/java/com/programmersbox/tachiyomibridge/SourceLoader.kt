package com.programmersbox.tachiyomibridge

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
import eu.kanade.tachiyomi.source.Source
import eu.kanade.tachiyomi.source.SourceFactory
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.online.HttpSource
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
        "tachiyomi.extension",
        "tachiyomi.extension.class",
    ) { t, a, p ->
        when (t) {
            is Source -> listOf(t)
            is SourceFactory -> t.createSources()
            else -> emptyList()
        }
            .filterIsInstance<HttpSource>()
            .filter { it.lang == "en" }
            .map { toSource(it, a, p) }
    }

    val toSource: (HttpSource, ApplicationInfo, PackageInfo) -> SourceInformation = { t, a, p ->
        SourceInformation(
            apiService = object : ApiService {
                override val baseUrl: String get() = t.baseUrl

                override val canScroll: Boolean = true

                override val serviceName: String get() = t.name + t.lang

                private fun MutableMap<String, Any>.storeSManga(sManga: SManga) {
                    set("smanga_title", sManga.title)
                    set("smanga_url", sManga.url)
                    set("smanga_artist", sManga.artist.orEmpty())
                    set("smanga_author", sManga.author.orEmpty())
                    set("smanga_thumbnail_url", sManga.thumbnail_url.orEmpty())
                    set("smanga_description", sManga.description.orEmpty())
                    set("smanga_genre", sManga.genre.orEmpty())
                    set("smanga_initialized", sManga.initialized)
                    set("smanga_status", sManga.status)
                }

                private fun MutableMap<String, Any>.retrieveSMana() = SManga.create().apply {
                    title = getOrDefault("smanga_title", "title").toString()
                    url = get("smanga_url").toString()
                    artist = get("smanga_artist").toString()
                    author = get("smanga_author").toString()
                    thumbnail_url = get("smanga_thumbnail_url").toString()
                    description = get("smanga_description").toString()
                    genre = get("smanga_genre").toString()
                    initialized = getOrDefault("smanga_initialized", false) as Boolean
                    status = (getOrDefault("smanga_status", 1) as Number).toInt()
                }

                override suspend fun recent(page: Int): List<ItemModel> {
                    return t.fetchLatestUpdates(page).toBlocking().first().mangas.map {
                        ItemModel(
                            title = it.title,
                            description = it.description.orEmpty(),
                            url = it.url,
                            imageUrl = it.thumbnail_url.orEmpty(),
                            source = this
                        ).also { item -> item.otherExtras.storeSManga(it) }
                    }
                }

                override suspend fun allList(page: Int): List<ItemModel> {
                    return t.fetchPopularManga(page).toBlocking().first().mangas.map {
                        ItemModel(
                            title = it.title,
                            description = it.description.orEmpty(),
                            url = it.url,
                            imageUrl = it.thumbnail_url.orEmpty(),
                            source = this
                        ).also { item -> item.otherExtras.storeSManga(it) }
                    }
                }

                override suspend fun search(
                    searchText: CharSequence,
                    page: Int,
                    list: List<ItemModel>
                ): List<ItemModel> {
                    return t.fetchSearchManga(page, searchText.toString(), t.getFilterList())
                        .toBlocking()
                        .first()
                        .mangas
                        .map {
                            ItemModel(
                                title = it.title,
                                description = it.description.orEmpty(),
                                url = it.url,
                                imageUrl = it.thumbnail_url.orEmpty(),
                                source = this
                            ).also { item -> item.otherExtras.storeSManga(it) }
                        }
                }

                private fun MutableMap<String, Any>.storeSChapter(sChapter: SChapter) {
                    set("schapter_url", sChapter.url)
                    set("schapter_name", sChapter.name)
                    set("schapter_chapter_number", sChapter.chapter_number)
                    set("schapter_date_upload", sChapter.date_upload)
                }

                private fun MutableMap<String, Any>.retrieveSChapter() = SChapter.create().apply {
                    url = getOrDefault("schapter_url", "").toString()
                    name = getOrDefault("schapter_name", "").toString()
                    chapter_number = getOrDefault("schapter_chapter_number", 1f) as Float
                    date_upload = getOrDefault("schapter_date_upload", 0L) as Long
                }

                override suspend fun itemInfo(model: ItemModel): InfoModel {
                    val f = model.otherExtras.retrieveSMana().apply {
                        if (url.isBlank() || url == "null") url = model.url
                        if (title.isBlank() || title == "null") title = model.title
                        if (description.isNullOrBlank() || title == "null") description =
                            model.description
                        if (thumbnail_url.isNullOrBlank()) thumbnail_url = model.imageUrl
                    }
                    val s = t.getMangaDetails(f)
                    return InfoModel(
                        title = model.title,
                        description = s.description.orEmpty(),
                        url = model.url,
                        imageUrl = model.imageUrl,
                        chapters = t.getChapterList(f).map {
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
                            ).also { item -> item.otherExtras.storeSChapter(it) }
                        },
                        genres = s.genre?.split(",").orEmpty(),
                        alternativeNames = emptyList(),
                        source = this
                    )
                }

                override suspend fun chapterInfo(chapterModel: ChapterModel): List<Storage> {
                    val s = chapterModel.otherExtras.retrieveSChapter()
                    return t.getPageList(s)
                        .map {
                            Storage(
                                link = it.imageUrl.orEmpty(),
                                source = chapterModel.url,
                                quality = "Good",
                                sub = "Yes"
                            )
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