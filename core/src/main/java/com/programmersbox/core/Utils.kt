package com.programmersbox.core

import com.programmersbox.models.ApiService
import com.programmersbox.models.ChapterModel
import com.programmersbox.models.InfoModel
import com.programmersbox.models.ItemModel

fun ApiService.ItemModel(
    title: String,
    description: String,
    url: String,
    imageUrl: String,
) = ItemModel(
    title = title,
    description = description,
    url = url,
    imageUrl = imageUrl,
    source = this
)

fun ApiService.InfoModel(
    title: String,
    description: String,
    url: String,
    imageUrl: String,
    chapters: List<ChapterModel>,
    genres: List<String>,
    alternativeNames: List<String>,
) = InfoModel(
    title = title,
    description = description,
    url = url,
    imageUrl = imageUrl,
    chapters = chapters,
    genres = genres,
    alternativeNames = alternativeNames,
    source = this
)

fun ApiService.ChapterModel(
    name: String,
    url: String,
    uploaded: String,
    sourceUrl: String,
) = ChapterModel(
    name = name,
    url = url,
    uploaded = uploaded,
    sourceUrl = sourceUrl,
    source = this
)