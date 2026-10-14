package io.jacob.episodive.core.network.model

import com.google.gson.annotations.SerializedName

data class KbsLiveResponse(
    @SerializedName("channel_item") val channelItem: List<KbsChannelItem>? = null,
)

data class KbsChannelItem(
    @SerializedName("service_url") val serviceUrl: String? = null,
)
