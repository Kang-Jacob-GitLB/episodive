package io.jacob.episodive.core.network.model

import com.google.gson.annotations.SerializedName

/** Radio Browser 의 스테이션 레코드 중 상세 화면이 쓰는 필드. 값은 모두 비어 있을 수 있다. */
data class StationDetailsResponse(
    @SerializedName("stationuuid") val stationUuid: String? = null,
    @SerializedName("homepage") val homepage: String? = null,
    @SerializedName("tags") val tags: String? = null,
    @SerializedName("codec") val codec: String? = null,
    @SerializedName("bitrate") val bitrate: Int? = null,
    @SerializedName("language") val language: String? = null,
)
