package io.jacob.episodive.core.database.model

import androidx.room.Embedded

/**
 * 즐겨찾기 행과 Radio Browser 메타를 LEFT JOIN 한 결과. 카탈로그 스테이션은 [meta] 가 null 이다.
 * 컬럼 별칭은 `meta_` 접두어를 쓴다(`StationDao` 의 쿼리 참고).
 */
data class FavoriteStationWithMeta(
    val stationId: String,
    val favoritedAtMillis: Long,
    @Embedded(prefix = "meta_") val meta: RadioBrowserStationEntity?,
)

/** 최근 행과 Radio Browser 메타를 LEFT JOIN 한 결과. [FavoriteStationWithMeta] 와 같은 규칙이다. */
data class RecentStationWithMeta(
    val stationId: String,
    val playedAtMillis: Long,
    @Embedded(prefix = "meta_") val meta: RadioBrowserStationEntity?,
)
