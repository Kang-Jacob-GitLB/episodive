package io.jacob.episodive.core.network.model

import com.google.gson.annotations.SerializedName

data class ChaptersResponse(
    @SerializedName("version") val version: String,
    @SerializedName("chapters") val chapters: List<ChapterResponse>,
)
