package io.jacob.episodive.core.data.repository

import io.jacob.episodive.core.data.radio.KoreanStationCatalog
import io.jacob.episodive.core.data.radio.StationCatalogEntry
import io.jacob.episodive.core.data.radio.mergeStationDetails
import io.jacob.episodive.core.database.dao.StationDao
import io.jacob.episodive.core.database.model.RecentStationEntity
import io.jacob.episodive.core.domain.repository.StationRepository
import io.jacob.episodive.core.model.OnAirProgram
import io.jacob.episodive.core.model.Station
import io.jacob.episodive.core.model.StationDetails
import io.jacob.episodive.core.model.StationKind
import io.jacob.episodive.core.model.StationStream
import io.jacob.episodive.core.network.datasource.BroadcasterStreamDataSource
import io.jacob.episodive.core.network.datasource.OnAirDataSource
import io.jacob.episodive.core.network.datasource.StationRemoteDataSource
import io.jacob.episodive.core.network.model.OnAirSource
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import timber.log.Timber
import java.time.Instant
import java.time.ZoneId
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Clock

class StationRepositoryImpl internal constructor(
    private val remoteDataSource: StationRemoteDataSource,
    private val broadcasterDataSource: BroadcasterStreamDataSource,
    private val onAirDataSource: OnAirDataSource,
    private val stationDao: StationDao,
    private val catalog: List<StationCatalogEntry>,
    private val ioDispatcher: CoroutineDispatcher,
    private val now: () -> Long = System::currentTimeMillis,
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

    override fun getStation(stationId: String): Station? = stationsById[stationId]?.station

    override suspend fun resolveStreamUrl(
        stationId: String,
        excluding: Set<String>,
    ): StationStream? = withContext(ioDispatcher) {
        val entry = stationsById[stationId] ?: return@withContext null

        // 후보마다 OkHttp 의 callTimeout 이 걸려 있어도 후보가 여럿이면 합이 길어진다. 재생 버튼을
        // 누른 사용자를 그만큼 기다리게 하지 않도록 전체에 상한을 두고, 넘으면 fallback 으로 간다.
        val resolved = withTimeoutOrNull(RESOLVE_TIMEOUT_MILLIS) {
            // 방송사 API 는 호출할 때마다 새 서명 URL 을 준다. Radio Browser 의 URL 은 크롤 시점 토큰이라
            // 만료돼 있는 경우가 많으므로 먼저 시도한다. 다만 느린 방송사가 Radio Browser 후보의 예산을
            // 다 먹지 않도록 별도 상한을 둔다.
            //
            // excluding 이 비어 있지 않다는 건 이미 실패한 주소를 들고 다시 조회한다는 뜻이다. 방송사는 매번
            // 새 토큰 문자열을 줘서 excluding 으로 걸러지지 않으므로, 이때는 건너뛰고 Radio Browser 로 간다.
            // 첫 Expired 재조회는 excluding 이 비어 있어(LiveStreamSupervisorImpl.reResolve) "만료 → 새 토큰"
            // 경로는 그대로 방송사를 탄다.
            if (excluding.isEmpty()) {
                entry.broadcasterSource?.let { source ->
                    try {
                        withTimeoutOrNull(BROADCASTER_TIMEOUT_MILLIS) {
                            broadcasterDataSource.resolve(source)
                        }?.let { return@withTimeoutOrNull it }
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        Timber.w(e, "broadcaster resolve failed. stationId: $stationId, source: $source")
                    }
                }
            }
            for (uuid in entry.radioBrowserIds) {
                try {
                    remoteDataSource.resolveStreamUrl(uuid)
                        ?.takeIf { it !in excluding }
                        ?.let { return@withTimeoutOrNull it }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Timber.w(e, "resolveStreamUrl failed. stationId: $stationId, uuid: $uuid")
                }
            }
            null
        }

        (resolved ?: entry.fallbackUrl?.takeIf { it !in excluding })?.let { StationStream(url = it, isHls = entry.isHls) }
    }

    private class CachedDetails(val details: StationDetails?, val fetchedAtMillis: Long)

    // 메모리 캐시만 둔다. 부가 정보라 앱을 다시 켜면 다시 받아도 되고, Room 스키마를 늘릴 값어치가 없다.
    private val detailsCache = mutableMapOf<String, CachedDetails>()
    private val detailsMutex = Mutex()

    override suspend fun getStationDetails(stationId: String): StationDetails? = withContext(ioDispatcher) {
        val entry = stationsById[stationId] ?: return@withContext null

        // 같은 스테이션을 동시에 부르면 한 번만 받도록 락 안에서 확인하고 받는다. 상세는 부가 정보라
        // 직렬화돼도 상관없다.
        detailsMutex.withLock {
            detailsCache[stationId]
                ?.takeIf { now() - it.fetchedAtMillis < DETAILS_TTL_MILLIS }
                ?.let { return@withLock it.details }

            val fetched = withTimeoutOrNull(DETAILS_TIMEOUT_MILLIS) {
                try {
                    val responses = remoteDataSource.getStationDetails(entry.radioBrowserIds)
                    Fetched(mergeStationDetails(entry.radioBrowserIds, responses))
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Timber.w(e, "getStationDetails failed. stationId: $stationId")
                    null
                }
            }
            // 실패·시간 초과는 캐시하지 않는다. 다음에 열 때 다시 시도한다. "정보 없음"(null)은 성공이라 캐시한다.
            fetched?.let { detailsCache[stationId] = CachedDetails(it.details, now()) }
            fetched?.details
        }
    }

    private class Fetched(val details: StationDetails?)

    private class CachedSchedule(
        val programs: List<OnAirProgram>,
        val dateKey: String,
        val fetchedAtMillis: Long,
    )

    // 편성표도 메모리 캐시만 둔다. 실패는 캐시하지 않는다.
    private val scheduleCache = mutableMapOf<String, CachedSchedule>()
    private val scheduleMutex = Mutex()

    override fun observeOnAir(stationId: String): Flow<OnAirProgram?> {
        val source = stationsById[stationId]?.onAirSource ?: return flowOf(null)
        val programs = onAirPrograms(stationId, source)
        // 곡은 프로그램 경계와 상관없이 바뀌므로 따로 폴링해 얹는다. 곡을 주는 방송사는 MBC 뿐이다.
        val flow = if (source is OnAirSource.Mbc) {
            combine(programs, onAirSongs(source)) { program, song -> program?.copy(song = song) }
        } else {
            programs
        }
        return flow.distinctUntilChanged()
    }

    private fun onAirPrograms(stationId: String, source: OnAirSource): Flow<OnAirProgram?> = flow {
        while (true) {
            val schedule = loadSchedule(stationId, source)
            val nowMillis = now()
            val current = schedule?.currentAt(nowMillis)
            emit(current)

            // 임시 편성으로 끝 시각이 어긋나도 따라가도록 끝 시각까지 가더라도 SCHEDULE_TTL 이상은 자지 않는다.
            // 현재 항목이 없으면 다음 항목의 시작까지, 다음도 없으면(SBS 는 지금 방송분뿐이다) 재시도 간격만큼 기다린다.
            val untilChange = when {
                schedule == null -> ONAIR_FAILURE_RETRY_MILLIS
                current != null -> (current.endMillis ?: Long.MAX_VALUE) - nowMillis
                else -> (schedule.nextStartAfter(nowMillis) ?: (nowMillis + ONAIR_FAILURE_RETRY_MILLIS)) - nowMillis
            }
            delay(untilChange.coerceIn(ONAIR_MIN_WAIT_MILLIS, SCHEDULE_TTL_MILLIS))
        }
    }

    private fun onAirSongs(source: OnAirSource.Mbc): Flow<String?> = flow {
        var last: String? = null
        while (true) {
            try {
                last = onAirDataSource.currentSong(source)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // 실패하면 직전 곡을 그대로 둔다.
                Timber.w(e, "currentSong failed. source: $source")
            }
            emit(last)
            delay(SONG_POLL_MILLIS)
        }
    }

    /**
     * 편성표를 캐시에서 주거나 받아 온다. 날짜가 바뀌었거나 [SCHEDULE_TTL_MILLIS] 가 지났거나, 캐시에 지금 시각의
     * 항목이 없는데 마지막으로 받은 지 [SCHEDULE_REFETCH_MIN_MILLIS] 가 지났으면 다시 받는다(SBS 는 지금 방송분
     * 하나뿐이라 끝나면 곧바로 필요하다). 실패면 null.
     */
    private suspend fun loadSchedule(stationId: String, source: OnAirSource): List<OnAirProgram>? {
        return scheduleMutex.withLock {
            val nowMillis = now()
            val dateKey = Instant.ofEpochMilli(nowMillis).atZone(SEOUL).toLocalDate().toString()
            scheduleCache[stationId]?.let { cached ->
                val age = nowMillis - cached.fetchedAtMillis
                val fresh = cached.dateKey == dateKey && age < SCHEDULE_TTL_MILLIS &&
                    (cached.programs.currentAt(nowMillis) != null || age < SCHEDULE_REFETCH_MIN_MILLIS)
                if (fresh) return@withLock cached.programs
            }
            try {
                onAirDataSource.schedule(source, nowMillis).also {
                    scheduleCache[stationId] = CachedSchedule(it, dateKey, nowMillis)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.w(e, "onAir schedule failed. stationId: $stationId, source: $source")
                null
            }
        }
    }

    private fun List<OnAirProgram>.currentAt(nowMillis: Long): OnAirProgram? =
        filter { (it.startMillis ?: return@filter false) <= nowMillis && nowMillis < (it.endMillis ?: return@filter false) }
            .maxByOrNull { it.startMillis ?: 0L }

    private fun List<OnAirProgram>.nextStartAfter(nowMillis: Long): Long? =
        mapNotNull { it.startMillis }.filter { it > nowMillis }.minOrNull()

    private companion object {
        const val RESOLVE_TIMEOUT_MILLIS = 15_000L
        const val BROADCASTER_TIMEOUT_MILLIS = 5_000L
        const val DETAILS_TIMEOUT_MILLIS = 10_000L
        const val DETAILS_TTL_MILLIS = 24 * 60 * 60 * 1000L
        const val SCHEDULE_TTL_MILLIS = 5 * 60 * 1000L
        const val SCHEDULE_REFETCH_MIN_MILLIS = 30_000L
        const val ONAIR_FAILURE_RETRY_MILLIS = 60_000L // 실패·편성 공백 뒤 재시도
        const val ONAIR_MIN_WAIT_MILLIS = 1_000L
        const val SONG_POLL_MILLIS = 45_000L
        val SEOUL: ZoneId = ZoneId.of("Asia/Seoul")
    }
}
