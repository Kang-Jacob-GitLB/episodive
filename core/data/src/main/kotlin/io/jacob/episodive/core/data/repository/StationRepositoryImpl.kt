package io.jacob.episodive.core.data.repository

import io.jacob.episodive.core.data.radio.KoreanStationCatalog
import io.jacob.episodive.core.data.radio.StationCatalogEntry
import io.jacob.episodive.core.database.dao.StationDao
import io.jacob.episodive.core.database.model.RecentStationEntity
import io.jacob.episodive.core.domain.repository.StationRepository
import io.jacob.episodive.core.model.Station
import io.jacob.episodive.core.model.StationKind
import io.jacob.episodive.core.model.StationStream
import io.jacob.episodive.core.network.datasource.StationRemoteDataSource
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import timber.log.Timber
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Clock

class StationRepositoryImpl internal constructor(
    private val remoteDataSource: StationRemoteDataSource,
    private val stationDao: StationDao,
    private val catalog: List<StationCatalogEntry>,
    private val ioDispatcher: CoroutineDispatcher,
) : StationRepository {

    private val stationsById = catalog.associateBy { it.station.id }

    override fun getStations(kind: StationKind): Flow<List<Station>> {
        return flowOf(
            catalog.map { it.station }.filter { it.kind == kind }
        )
    }

    override fun getFavoriteStations(): Flow<List<Station>> {
        return stationDao.getFavoriteStationIds().map { it.toStations() }
    }

    override fun isFavorite(stationId: String): Flow<Boolean> {
        return stationDao.isFavorite(stationId)
    }

    override suspend fun toggleFavorite(stationId: String): Boolean {
        return stationDao.toggleFavorite(stationId)
    }

    override fun getRecentStations(limit: Int): Flow<List<Station>> {
        // 카탈로그에 없는 id 가 섞여 있어도 limit 개를 채우도록 조인 뒤에 자른다.
        return stationDao.getRecentStationIds().map { it.toStations().take(limit) }
    }

    override suspend fun markPlayed(stationId: String) {
        stationDao.recordPlayed(
            recent = RecentStationEntity(
                stationId = stationId,
                playedAtMillis = Clock.System.now().toEpochMilliseconds(),
            ),
            keep = StationRepository.MAX_RECENT_STATIONS,
        )
    }

    /**
     * 저장된 id 를 카탈로그와 조인한다. 카탈로그에서 빠진(종료·개편된) 스테이션의 id 는 DB 에 남아
     * 있을 수 있는데, 재생할 방법이 없으므로 에러로 올리지 않고 조용히 뺀다. 순서는 [ids] 를 따른다.
     */
    private fun List<String>.toStations(): List<Station> {
        return mapNotNull { stationsById[it]?.station }
    }

    override suspend fun resolveStreamUrl(stationId: String): StationStream? = withContext(ioDispatcher) {
        val entry = stationsById[stationId] ?: return@withContext null

        // 후보마다 OkHttp 의 callTimeout 이 걸려 있어도 후보가 여럿이면 합이 길어진다. 재생 버튼을
        // 누른 사용자를 그만큼 기다리게 하지 않도록 전체에 상한을 두고, 넘으면 fallback 으로 간다.
        val resolved = withTimeoutOrNull(RESOLVE_TIMEOUT_MILLIS) {
            for (uuid in entry.radioBrowserIds) {
                try {
                    remoteDataSource.resolveStreamUrl(uuid)?.let { return@withTimeoutOrNull it }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Timber.w(e, "resolveStreamUrl failed. stationId: $stationId, uuid: $uuid")
                }
            }
            null
        }

        (resolved ?: entry.fallbackUrl)?.let { StationStream(url = it, isHls = entry.isHls) }
    }

    private companion object {
        const val RESOLVE_TIMEOUT_MILLIS = 15_000L
    }
}
