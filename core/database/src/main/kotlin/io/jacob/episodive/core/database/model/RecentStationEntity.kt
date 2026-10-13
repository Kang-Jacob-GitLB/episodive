package io.jacob.episodive.core.database.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 최근 들은 라이브 스테이션. 같은 스테이션은 한 행이고 [playedAtMillis] 만 갱신된다.
 *
 * [stationId] 의미와 스트림 URL 을 저장하지 않는 이유는 [FavoriteStationEntity] 와 같다.
 *
 * 시각은 전역 `InstantConverter`(초 단위)를 쓰지 않고 epoch 밀리초 `Long` 으로 직접 저장한다. 초
 * 단위면 같은 초 안의 연속 탭이 동점이 되어 순서를 잃는다. 컨버터를 바꾸면 기존 표 전부에 영향이
 * 가므로 이 표만 밀리초를 쓴다.
 */
@Entity(
    tableName = "recent_stations",
    indices = [
        Index(value = ["playedAtMillis"]),
    ]
)
data class RecentStationEntity(
    @PrimaryKey val stationId: String,
    val playedAtMillis: Long,
)
