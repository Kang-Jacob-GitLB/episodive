package io.jacob.episodive.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import io.jacob.episodive.core.database.model.FavoriteStationEntity
import io.jacob.episodive.core.database.model.FavoriteStationWithMeta
import io.jacob.episodive.core.database.model.RadioBrowserStationEntity
import io.jacob.episodive.core.database.model.RecentStationEntity
import io.jacob.episodive.core.database.model.RecentStationWithMeta
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlin.time.Clock

/**
 * 스테이션 id 와, 있으면 Radio Browser 메타를 돌려준다. `Station` 으로 만드는 일(카탈로그 조인)은
 * 리포지토리가 한다.
 */
@Dao
interface StationDao {
    @Query("SELECT stationId FROM favorite_stations ORDER BY favoritedAtMillis DESC, stationId DESC")
    fun getFavoriteStationIds(): Flow<List<String>>

    @Query("SELECT EXISTS(SELECT 1 FROM favorite_stations WHERE stationId = :stationId)")
    fun isFavorite(stationId: String): Flow<Boolean>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addFavorite(favorite: FavoriteStationEntity)

    @Query("DELETE FROM favorite_stations WHERE stationId = :stationId")
    suspend fun removeFavorite(stationId: String)

    /** 바뀐 뒤의 상태를 돌려준다. true 면 즐겨찾기됨. */
    @Transaction
    suspend fun toggleFavorite(stationId: String): Boolean {
        return if (isFavorite(stationId).first()) {
            removeFavorite(stationId)
            deleteOrphanRemoteStations()
            false
        } else {
            addFavorite(
                FavoriteStationEntity(
                    stationId = stationId,
                    favoritedAtMillis = Clock.System.now().toEpochMilliseconds(),
                )
            )
            true
        }
    }

    /** [toggleFavorite] 와 같되, 즐겨찾기를 더할 때는 [meta] 를 같은 트랜잭션에서 upsert 한다. */
    @Transaction
    suspend fun toggleFavoriteWithMeta(meta: RadioBrowserStationEntity): Boolean {
        return if (isFavorite(meta.stationId).first()) {
            removeFavorite(meta.stationId)
            deleteOrphanRemoteStations()
            false
        } else {
            upsertRemoteStation(meta)
            addFavorite(
                FavoriteStationEntity(
                    stationId = meta.stationId,
                    favoritedAtMillis = Clock.System.now().toEpochMilliseconds(),
                )
            )
            true
        }
    }

    /**
     * 즐겨찾기 목록과 메타. 정렬은 [getFavoriteStationIds] 와 같다.
     * 메타가 없는 행(카탈로그 스테이션)도 그대로 나온다.
     */
    @Query(
        "SELECT f.stationId AS stationId, f.favoritedAtMillis AS favoritedAtMillis, " +
            "m.stationId AS meta_stationId, m.uuid AS meta_uuid, m.name AS meta_name, " +
            "m.region AS meta_region, m.language AS meta_language, m.isHls AS meta_isHls, " +
            "m.savedAtMillis AS meta_savedAtMillis " +
            "FROM favorite_stations AS f LEFT JOIN radio_browser_stations AS m ON m.stationId = f.stationId " +
            "ORDER BY f.favoritedAtMillis DESC, f.stationId DESC"
    )
    fun getFavoriteStationsWithMeta(): Flow<List<FavoriteStationWithMeta>>

    /** 표가 보관 상한으로 묶여 있어 LIMIT 을 두지 않는다. 자르는 일은 카탈로그 조인 뒤 리포지토리가 한다. */
    @Query("SELECT stationId FROM recent_stations ORDER BY playedAtMillis DESC, stationId DESC")
    fun getRecentStationIds(): Flow<List<String>>

    @Upsert
    suspend fun upsertRecent(recent: RecentStationEntity)

    /** 최근 목록과 메타. 정렬은 [getRecentStationIds] 와 같다. */
    @Query(
        "SELECT r.stationId AS stationId, r.playedAtMillis AS playedAtMillis, " +
            "m.stationId AS meta_stationId, m.uuid AS meta_uuid, m.name AS meta_name, " +
            "m.region AS meta_region, m.language AS meta_language, m.isHls AS meta_isHls, " +
            "m.savedAtMillis AS meta_savedAtMillis " +
            "FROM recent_stations AS r LEFT JOIN radio_browser_stations AS m ON m.stationId = r.stationId " +
            "ORDER BY r.playedAtMillis DESC, r.stationId DESC"
    )
    fun getRecentStationsWithMeta(): Flow<List<RecentStationWithMeta>>

    @Upsert
    suspend fun upsertRemoteStation(station: RadioBrowserStationEntity)

    /** 즐겨찾기·최근 어디에도 없는 Radio Browser 메타를 지운다. */
    @Query(
        "DELETE FROM radio_browser_stations WHERE stationId NOT IN " +
            "(SELECT stationId FROM favorite_stations) AND stationId NOT IN " +
            "(SELECT stationId FROM recent_stations)"
    )
    suspend fun deleteOrphanRemoteStations()

    /** 최신 [keep] 개만 남긴다. 정렬은 [getRecentStationIds] 와 같아야 보이는 것과 남는 것이 일치한다. */
    @Query(
        "DELETE FROM recent_stations WHERE stationId NOT IN " +
            "(SELECT stationId FROM recent_stations ORDER BY playedAtMillis DESC, stationId DESC LIMIT :keep)"
    )
    suspend fun trimRecent(keep: Int)

    /** 기록과 정리를 한 트랜잭션으로 묶어, Flow 가 정리 전의 중간 상태를 한 번 더 내보내지 않게 한다. */
    @Transaction
    suspend fun recordPlayed(recent: RecentStationEntity, keep: Int) {
        upsertRecent(recent)
        trimRecent(keep)
        deleteOrphanRemoteStations()
    }

    /** [recordPlayed] 와 같되 [meta] 를 같은 트랜잭션에서 upsert 한다. 정리는 맨 끝이라 방금 넣은 메타는 남는다. */
    @Transaction
    suspend fun recordPlayedWithMeta(meta: RadioBrowserStationEntity, recent: RecentStationEntity, keep: Int) {
        upsertRemoteStation(meta)
        recordPlayed(recent, keep)
    }
}
