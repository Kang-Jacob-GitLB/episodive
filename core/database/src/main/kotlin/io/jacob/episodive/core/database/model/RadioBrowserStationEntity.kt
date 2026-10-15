package io.jacob.episodive.core.database.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 즐겨찾기·최근 목록에 오른 Radio Browser 스테이션의 메타. 카탈로그(코드)에 없는 해외 스테이션을
 * 목록에서 다시 그리려면 이름 등이 필요해서 둔다.
 *
 * [stationId] 는 `rb:{uuid}` 형태의 `Station.id` 이고 [uuid] 는 그 뒷부분이다. 즐겨찾기·최근을
 * 더할 때만 upsert 하고, 둘 어디에도 없게 되면 지운다(`StationDao.deleteOrphanRemoteStations`).
 * 원격 캐시가 아니라 사용자 데이터에 딸린 표라 `cachedAt`/그룹 키 대신 [savedAtMillis] 만 둔다.
 * **스트림 URL 은 저장하지 않는다.** [FavoriteStationEntity] 와 같은 이유로, 토큰이 붙어 만료되므로
 * 재생 직전에 새로 받는다.
 *
 * 시각은 [FavoriteStationEntity] 처럼 epoch 밀리초 `Long` 이다.
 */
@Entity(tableName = "radio_browser_stations")
data class RadioBrowserStationEntity(
    @PrimaryKey val stationId: String,
    val uuid: String,
    val name: String,
    val region: String?,
    val language: String?,
    val isHls: Boolean,
    val savedAtMillis: Long,
)
