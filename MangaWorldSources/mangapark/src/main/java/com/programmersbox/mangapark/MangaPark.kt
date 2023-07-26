package com.programmersbox.mangapark

import android.annotation.SuppressLint
import app.cash.zipline.EngineApi
import app.cash.zipline.QuickJs
import com.programmersbox.mangaworldsources.GET
import com.programmersbox.mangaworldsources.MangaUtils.headers
import com.programmersbox.models.*
import com.programmersbox.source_utilities.NetworkHelper
import com.programmersbox.source_utilities.asJsoup
import com.programmersbox.source_utilities.cloudflare
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.Request
import okhttp3.Response
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.text.SimpleDateFormat
import java.util.*

object MangaPark : ApiService, KoinComponent {

    override val baseUrl = "https://mangapark.net"

    override val serviceName: String get() = "MANGA_PARK"

    private val helper: NetworkHelper by inject()

    private fun String.v3Url() = baseUrl

    override suspend fun search(
        searchText: CharSequence,
        page: Int,
        list: List<ItemModel>
    ): List<ItemModel> {
        return cloudflare(helper, "${baseUrl.v3Url()}/search?word=$searchText&page=$page").execute()
            .asJsoup()
            .browseToItemModel("div#search-list div.col")
    }

    override suspend fun allList(page: Int): List<ItemModel> {
        return cloudflare(helper, "${baseUrl.v3Url()}/browse?sort=d007&page=$page").execute()
            .asJsoup().browseToItemModel()
    }

    override suspend fun recent(page: Int): List<ItemModel> {
        return cloudflare(helper, "${baseUrl.v3Url()}/browse?sort=update&page=$page").execute()
            .asJsoup().browseToItemModel()
    }

    private fun Document.browseToItemModel(query: String = "div#subject-list div.col") =
        select(query)
            .map {
                ItemModel(
                    title = it.select("a.fw-bold").text(),
                    description = it.select("div.limit-html").text(),
                    url = it.select("a.fw-bold").attr("abs:href"),
                    imageUrl = it.select("a.position-relative img").attr("abs:src"),
                    source = this@MangaPark
                )
            }

    override suspend fun itemInfo(model: ItemModel): InfoModel {
        val doc = cloudflare(helper, model.url.v3Url()).execute().asJsoup()
        return try {
            val infoElement = doc.select("div#mainer div.container-fluid")
            InfoModel(
                title = model.title,
                description = model.description,
                url = model.url,
                imageUrl = model.imageUrl,
                chapters = chapterListParse(
                    helper.cloudflareClient.newCall(chapterListRequest(model)).execute(),
                    model.url.v3Url()
                ),
                genres = infoElement.select("div.attr-item:contains(genres) span span")
                    .map { it.text().trim() },
                alternativeNames = emptyList(),
                source = this
            )
        } catch (e: Exception) {
            e.printStackTrace()
            val genres = mutableListOf<String>()
            val alternateNames = mutableListOf<String>()
            doc.select(".attr > tbody > tr").forEach {
                when (it.getElementsByTag("th").first()!!.text().trim()
                    .lowercase(Locale.getDefault())) {
                    "genre(s)" -> genres.addAll(it.getElementsByTag("a").map(Element::text))
                    "alternative" -> alternateNames.addAll(it.text().split("l"))
                }
            }
            InfoModel(
                title = model.title,
                description = doc.select("p.summary").text(),
                url = model.url,
                imageUrl = model.imageUrl,
                chapters = chapterListParse(doc, model.url),
                genres = genres,
                alternativeNames = alternateNames,
                source = this
            )
        }
    }

    private fun chapterListRequest(manga: ItemModel): Request {
        return GET(manga.url)
    }

    private fun chapterListParse(response: Response, mangaUrl: String): List<ChapterModel> {
        val f = "div.p-2:not(:has(.px-3))"
        return response.asJsoup()
            .select("div.episode-list #chap-index")
            .flatMap { it.select(f).map { chapterFromElement(it) } }
            .map {
                ChapterModel(
                    name = it.name,
                    url = it.url,
                    uploaded = it.originalDate,
                    sourceUrl = mangaUrl,
                    source = this
                ).apply { uploadedTime = it.dateUploaded }
            }
    }

    private fun chapterListParse(response: Document, mangaUrl: String): List<ChapterModel> {
        val f = "div.p-2:not(:has(.px-3))"
        return response
            .select("div.episode-list #chap-index")
            .flatMap { it.select(f).map { chapterFromElement(it) } }
            .map {
                ChapterModel(
                    name = it.name,
                    url = it.url,
                    uploaded = it.originalDate,
                    sourceUrl = mangaUrl,
                    source = this
                ).apply { uploadedTime = it.dateUploaded }
            }
    }

    private class SChapter {
        var url: String = ""
        var name: String = ""
        var chapterNumber: Float = 0f
        var dateUploaded: Long? = null
        var originalDate: String = ""
    }

    private fun chapterFromElement(element: Element): SChapter {
        val urlElement = element.select("a.ms-3")
        val time = element.select("div.extra > i.ps-2").text()
        return SChapter().apply {
            name = urlElement.text().removePrefix("Ch").trim()//urlElement.text()
            chapterNumber = urlElement.attr("href").substringAfterLast("/").toFloatOrNull() ?: 0f
            if (time != "") {
                dateUploaded = parseDate(time)
            }
            originalDate = time
            url = baseUrl.v3Url() + urlElement.attr("href")
        }
    }

    private fun chapterFromElement(element: Element, lastNum: Float): SChapter {
        fun Float.incremented() = this + .00001F
        fun Float?.orIncrementLastNum() =
            if (this == null || this < lastNum) lastNum.incremented() else this

        return SChapter().apply {
            url = element.select(".tit > a").first()!!.attr("href").replaceAfterLast("/", "")
            name = element.select(".tit > a").first()!!.text()
            // Get the chapter number or create a unique one if it's not available
            chapterNumber = Regex("""\b\d+\.?\d?\b""").findAll(name)
                .toList()
                .map { it.value.toFloatOrNull() }
                .let { nums ->
                    when {
                        nums.count() == 1 -> nums[0].orIncrementLastNum()
                        nums.count() >= 2 -> nums[1].orIncrementLastNum()
                        else -> lastNum.incremented()
                    }
                }
            dateUploaded =
                element.select(".time").firstOrNull()?.text()?.trim()?.let { parseDate(it) }
            originalDate = element.select(".time").firstOrNull()?.text()?.trim().toString()
        }
    }

    private val cryptoJS by lazy {
        helper.client.newCall(GET(cryptoJSUrl, headers)).execute().body.string()
    }

    private const val cryptoJSUrl =
        "https://cdnjs.cloudflare.com/ajax/libs/crypto-js/4.0.0/crypto-js.min.js"

    private val dateFormat = SimpleDateFormat("MMM d, yyyy, HH:mm a", Locale.ENGLISH)
    private val dateFormatTimeOnly = SimpleDateFormat("HH:mm a", Locale.ENGLISH)

    @SuppressLint("DefaultLocale")
    private fun parseDate(date: String): Long? {
        val lcDate = date.lowercase()
        if (lcDate.endsWith("ago")) return parseRelativeDate(lcDate)

        // Handle 'yesterday' and 'today'
        var relativeDate: Calendar? = null
        if (lcDate.startsWith("yesterday")) {
            relativeDate = Calendar.getInstance()
            relativeDate.add(Calendar.DAY_OF_MONTH, -1) // yesterday
        } else if (lcDate.startsWith("today")) {
            relativeDate = Calendar.getInstance()
        }

        relativeDate?.let {
            // Since the date is not specified, it defaults to 1970!
            val time = dateFormatTimeOnly.parse(lcDate.substringAfter(' '))
            val cal = Calendar.getInstance()
            cal.time = time!!

            // Copy time to relative date
            it.set(Calendar.HOUR_OF_DAY, cal.get(Calendar.HOUR_OF_DAY))
            it.set(Calendar.MINUTE, cal.get(Calendar.MINUTE))
            return it.timeInMillis
        }

        return dateFormat.parse(lcDate)?.time
    }

    /**
     * Parses dates in this form:
     * `11 days ago`
     */
    private fun parseRelativeDate(date: String): Long? {
        val trimmedDate = date.split(" ")

        if (trimmedDate[2] != "ago") return null

        val number = when (trimmedDate[0]) {
            "a" -> 1
            else -> trimmedDate[0].toIntOrNull() ?: return null
        }
        val unit = trimmedDate[1].removeSuffix("s") // Remove 's' suffix

        val now = Calendar.getInstance()

        // Map English unit to Java unit
        val javaUnit = when (unit) {
            "year" -> Calendar.YEAR
            "month" -> Calendar.MONTH
            "week" -> Calendar.WEEK_OF_MONTH
            "day" -> Calendar.DAY_OF_MONTH
            "hour" -> Calendar.HOUR
            "minute" -> Calendar.MINUTE
            "second" -> Calendar.SECOND
            else -> return null
        }

        now.add(javaUnit, -number)

        return now.timeInMillis
    }

    override suspend fun sourceByUrl(url: String): ItemModel {
        val doc = cloudflare(helper, url).execute().asJsoup()
        val infoElement = doc.select("div#mainer div.container-fluid")
        return ItemModel(
            title = infoElement.select("h3.item-title").text(),
            description = infoElement.select("div.limit-height-body")
                .select("h5.text-muted, div.limit-html")
                .joinToString("\n\n", transform = Element::text),
            url = url,
            imageUrl = infoElement.select("div.detail-set div.attr-cover img").attr("abs:src"),
            source = this
        )
    }

    @OptIn(EngineApi::class)
    override suspend fun chapterInfo(chapterModel: ChapterModel): List<Storage> {
        val script = cloudflare(helper, chapterModel.url).execute().asJsoup()
            .select("script:containsData(imgHttpLis):containsData(amWord):containsData(amPass)")
            .html()
        val imgHttpLisString =
            script.substringAfter("const imgHttpLis =").substringBefore(";").trim()
        val imgHttpLis =
            Json.parseToJsonElement(imgHttpLisString).jsonArray.map { it.jsonPrimitive.content }
        val amWord = script.substringAfter("const amWord =").substringBefore(";").trim()
        val amPass = script.substringAfter("const amPass =").substringBefore(";").trim()

        val decryptScript =
            cryptoJS + "CryptoJS.AES.decrypt($amWord, $amPass).toString(CryptoJS.enc.Utf8);"

        val imgAccListString = QuickJs.create().use { it.evaluate(decryptScript).toString() }
        val imgAccList =
            Json.parseToJsonElement(imgAccListString).jsonArray.map { it.jsonPrimitive.content }

        return imgHttpLis.zip(imgAccList).mapIndexed { i, (imgUrl, imgAcc) -> "$imgUrl?$imgAcc" }
            .map { Storage(link = it, source = chapterModel.url, quality = "Good", sub = "Yes") }
    }

    override val canScroll: Boolean = true
}