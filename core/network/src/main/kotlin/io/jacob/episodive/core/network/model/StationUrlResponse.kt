package io.jacob.episodive.core.network.model

import com.google.gson.annotations.SerializedName

data class StationUrlResponse(
    @SerializedName("ok") val ok: Boolean,
    @SerializedName("message") val message: String? = null,
    @SerializedName("stationuuid") val stationUuid: String? = null,
    @SerializedName("name") val name: String? = null,
    @SerializedName("url") val url: String? = null,
)
