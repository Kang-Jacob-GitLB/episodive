package io.jacob.episodive.core.network.model

import com.google.gson.annotations.SerializedName

data class ChapterResponse(
    @SerializedName("title") val title: String,
    @SerializedName("startTime") val startTime: Int,
    @SerializedName("endTime") val endTime: Int,
)
