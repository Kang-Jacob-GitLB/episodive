package io.jacob.episodive.core.model

/**
 * 한 국가의 스테이션 목록과 그 출처.
 *
 * @property countryCode 목록의 기준이 된 국가(ISO alpha-2 대문자). 한국은 카탈로그라 `KR`.
 */
data class StationListing(
    val countryCode: String,
    val source: StationListingSource,
    val stations: List<Station>,
)

enum class StationListingSource {
    /** 앱이 직접 고른 카탈로그(한국). 방송사 API·편성·이미지를 쓴다. */
    Curated,

    /** Radio Browser 의 국가별 인기 목록. */
    RadioBrowser,
}
