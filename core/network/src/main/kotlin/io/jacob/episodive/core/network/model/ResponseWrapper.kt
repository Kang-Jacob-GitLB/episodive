package io.jacob.episodive.core.network.model

import com.google.gson.annotations.SerializedName

data class ResponseWrapper<T>(
    @SerializedName("status") val status: String,
    @SerializedName(value = "feed", alternate = ["item", "episode"]) val data: T?,
    @SerializedName("description") val description: String,
)
