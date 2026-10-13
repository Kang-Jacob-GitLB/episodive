package io.jacob.episodive.core.database.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 즐겨찾기한 라이브 스테이션. 사용자 데이터라 원격 캐시용 `cachedAt`/그룹 키는 두지 않는다.
 *
 * [stationId] 는 `Station.id`(카탈로그 slug)다. 카탈로그에 대한 외래키는 없다 — 카탈로그는 DB 가
 * 아니라 코드에 있고, 카탈로그에서 빠진 id 는 리포지토리가 조회 때 걸러낸다.
 * **스트림 URL 은 저장하지 않는다.** 토큰이 붙어 만료되므로 재생 직전에 새로 받는다.
 *
 * 시각은 전역 `InstantConverter`(초 단위)를 쓰지 않고 epoch 밀리초 `Long` 으로 직접 저장한다. 초
 * 단위면 같은 초 안의 연속 탭이 동점이 되어 순서를 잃는다. 컨버터를 바꾸면 기존 표 전부에 영향이
 * 가므로 이 표만 밀리초를 쓴다.
 */
@Entity(
    tableName = "favorite_stations",
    indices = [
        Index(value = ["favoritedAtMillis"]),
    ]
)
data class FavoriteStationEntity(
    @PrimaryKey val stationId: String,
    val favoritedAtMillis: Long,
)
