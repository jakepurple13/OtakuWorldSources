package com.programmersbox.animeworldsources

import com.programmersbox.models.ApiService
import com.programmersbox.models.ItemModel
import org.jsoup.nodes.Document

abstract class ShowApi(
    override val baseUrl: String,
    internal val allPath: String,
    internal val recentPath: String
) : ApiService {

    protected fun recentPath(page: Int = 1): Document = "$baseUrl/$recentPath${recentPage(page)}".toJsoup()
    protected fun all(page: Int = 1): Document = "$baseUrl/$allPath${allPage(page)}".toJsoup()

    protected open fun recentPage(page: Int): String = ""
    protected open fun allPage(page: Int): String = ""

    protected fun searchListNonSingle(searchText: CharSequence, page: Int, list: List<ItemModel>): List<ItemModel> =
        if (searchText.isEmpty()) list else list.filter { it.title.contains(searchText, true) }
}