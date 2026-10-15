package io.jacob.episodive.core.network.model

import com.google.gson.annotations.SerializedName

/**
 * Radio Browser 의 스테이션 레코드 중 상세 화면과 국가별 목록이 쓰는 필드. 값은 모두 비어 있을 수 있다.
 *
 * 서버가 주는 `is_https` 는 일부러 받지 않는다 — 클라이언트가 [urlResolved] 로 직접 판정한다.
 */
data class StationDetailsResponse(
    @SerializedName("stationuuid") val stationUuid: String? = null,
    @SerializedName("homepage") val homepage: String? = null,
    @SerializedName("tags") val tags: String? = null,
    @SerializedName("codec") val codec: String? = null,
    @SerializedName("bitrate") val bitrate: Int? = null,
    @SerializedName("language") val language: String? = null,
    @SerializedName("name") val name: String? = null,
    @SerializedName("url_resolved") val urlResolved: String? = null,
    @SerializedName("state") val state: String? = null,
    @SerializedName("languagecodes") val languageCodes: String? = null,
    @SerializedName("hls") val hls: Int? = null,
    @SerializedName("lastcheckok") val lastCheckOk: Int? = null,
    @SerializedName("ssl_error") val sslError: Int? = null,
    @SerializedName("clickcount") val clickCount: Int? = null,
)
