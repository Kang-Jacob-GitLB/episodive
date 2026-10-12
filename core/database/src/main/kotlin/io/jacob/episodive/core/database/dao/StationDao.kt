package io.jacob.episodive.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import io.jacob.episodive.core.database.model.FavoriteStationEntity
import io.jacob.episodive.core.database.model.RecentStationEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlin.time.Clock

/** 스테이션 id 만 돌려준다. `Station` 으로 만드는 일(카탈로그 조인)은 리포지토리가 한다. */
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

    /** 표가 보관 상한으로 묶여 있어 LIMIT 을 두지 않는다. 자르는 일은 카탈로그 조인 뒤 리포지토리가 한다. */
    @Query("SELECT stationId FROM recent_stations ORDER BY playedAtMillis DESC, stationId DESC")
    fun getRecentStationIds(): Flow<List<String>>

    @Upsert
    suspend fun upsertRecent(recent: RecentStationEntity)

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
    }
}
