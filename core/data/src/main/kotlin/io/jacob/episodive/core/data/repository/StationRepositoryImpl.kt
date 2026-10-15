package io.jacob.episodive.core.data.repository

import io.jacob.episodive.core.data.radio.KoreanStationCatalog
import io.jacob.episodive.core.data.radio.RemoteStation
import io.jacob.episodive.core.data.radio.StationCatalogEntry
import io.jacob.episodive.core.data.radio.isRadioBrowserStationId
import io.jacob.episodive.core.data.radio.mergeStationDetails
import io.jacob.episodive.core.data.radio.normalizeSchedule
import io.jacob.episodive.core.data.radio.radioBrowserUuid
import io.jacob.episodive.core.data.radio.toRemoteStations
import io.jacob.episodive.core.database.dao.StationDao
import io.jacob.episodive.core.database.model.RadioBrowserStationEntity
import io.jacob.episodive.core.database.model.RecentStationEntity
import io.jacob.episodive.core.domain.repository.StationRepository
import io.jacob.episodive.core.model.OnAirProgram
import io.jacob.episodive.core.model.OnAirRefresh
import io.jacob.episodive.core.model.Station
import io.jacob.episodive.core.model.StationDetails
import io.jacob.episodive.core.model.StationKind
import io.jacob.episodive.core.model.StationListing
import io.jacob.episodive.core.model.StationListingSource
import io.jacob.episodive.core.model.StationSchedule
import io.jacob.episodive.core.model.StationStream
import io.jacob.episodive.core.network.datasource.BroadcasterStreamDataSource
import io.jacob.episodive.core.network.datasource.OnAirDataSource
import io.jacob.episodive.core.network.datasource.StationRemoteDataSource
import io.jacob.episodive.core.network.model.OnAirSongLog
import io.jacob.episodive.core.network.model.OnAirSource
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import timber.log.Timber
import java.io.IOException
import java.time.Instant
import java.time.ZoneId
import java.util.concurrent.ConcurrentHashMap
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

    private class CachedListing(val stations: List<RemoteStation>, val fetchedAtMillis: Long)

    // 해외 목록은 메모리 캐시만 둔다. 인기 순위라 앱을 다시 켜면 다시 받아도 되고, 토큰이 붙은 주소를 DB 에 남기지 않는다.
    private val listingCache = ConcurrentHashMap<String, CachedListing>()

    // 국가별 락. 전역 락이면 느린 국가 하나가 다른 국가의 갱신까지 막는다.
    private val listingMutexes = ConcurrentHashMap<String, Mutex>()

    // 지금까지 목록에서 본 해외 스테이션. 목록에서 고른 스테이션을 재생·즐겨찾기할 때 주소(재생 대체)와 메타(저장)를 얻는다.
    private val knownRemote = ConcurrentHashMap<String, RemoteStation>()

    override fun getStationListing(kind: StationKind, countryCode: String?): Flow<StationListing> {
        val country = countryCode?.trim()?.uppercase()?.takeIf { COUNTRY_CODE.matches(it) } ?: CURATED_COUNTRY
        if (country == CURATED_COUNTRY || kind != StationKind.RADIO) {
            return flowOf(
                StationListing(
                    countryCode = CURATED_COUNTRY,
                    source = StationListingSource.Curated,
                    stations = catalog.map { it.station }.filter { it.kind == kind },
                )
            )
        }
        return flow {
            emit(
                StationListing(
                    countryCode = country,
                    source = StationListingSource.RadioBrowser,
                    stations = loadCountry(country).map { it.station },
                )
            )
        }
    }

    /**
     * 국가의 목록을 캐시에서 주거나 받아 온다. [LISTING_TTL_MILLIS] 안이면 그대로 쓰고, 지났으면 다시 받는다.
     * 받다 실패하면(시간 초과 포함) 이전 값이 있으면 그것을 주고 캐시 시각은 갱신하지 않아 다음에 다시 시도한다.
     * 이전 값이 없으면 예외를 던진다 — 실패를 빈 목록으로 캐시하지 않는다.
     */
    private suspend fun loadCountry(country: String): List<RemoteStation> = withContext(ioDispatcher) {
        listingMutexes.getOrPut(country) { Mutex() }.withLock {
            val cached = listingCache[country]
            if (cached != null && now() - cached.fetchedAtMillis < LISTING_TTL_MILLIS) return@withLock cached.stations
            try {
                val fetched = withTimeoutOrNull(LISTING_TIMEOUT_MILLIS) {
                    remoteDataSource.getStationsByCountry(country).toRemoteStations()
                } ?: throw IOException("station listing timed out. country: $country")
                listingCache[country] = CachedListing(fetched, now())
                fetched.forEach { knownRemote[it.station.id] = it }
                fetched
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.w(e, "getStationsByCountry failed. country: $country")
                cached?.stations ?: throw e
            }
        }
    }

    // 조인과 Room 조회에서 본 메타. 재시작 뒤처럼 목록(knownRemote)을 본 적 없는 스테이션도 해제 → 실행 취소로 다시
    // 더할 수 있게 한다. 메타 없이 더하면 조인에서 버려져 즐겨찾기가 조용히 사라진다.
    private val rememberedMeta = ConcurrentHashMap<String, RadioBrowserStationEntity>()

    override fun getFavoriteStations(): Flow<List<Station>> {
        return stationDao.getFavoriteStationsWithMeta().map { rows ->
            rows.mapNotNull { stationOf(it.stationId, it.meta) }
        }
    }

    override fun isFavorite(stationId: String): Flow<Boolean> {
        return stationDao.isFavorite(stationId)
    }

    override suspend fun toggleFavorite(stationId: String): Boolean {
        if (!stationId.isRadioBrowserStationId()) return stationDao.toggleFavorite(stationId)
        val meta = metaOf(stationId)
        if (meta != null) return stationDao.toggleFavoriteWithMeta(meta)
        // 메타를 끝내 못 얻은 해외 스테이션은 더하지 않는다(메타 없는 행은 목록에서 버려진다). 해제는 허용한다.
        Timber.w("no meta for remote station. stationId: $stationId")
        return if (stationDao.isFavorite(stationId).first()) stationDao.toggleFavorite(stationId) else false
    }

    /** 메타를 목록(메모리) → 앞서 본 메타 → Room 순으로 찾는다. 없으면 null. */
    private suspend fun metaOf(stationId: String): RadioBrowserStationEntity? =
        knownRemote[stationId]?.toEntity()
            ?: rememberedMeta[stationId]
            ?: roomMetaOf(stationId)

    /** Room 의 메타 한 건. 읽기에 실패해도 호출한 쪽의 흐름(재생·표시)을 막지 않으려 null 로 돌린다. */
    private suspend fun roomMetaOf(stationId: String): RadioBrowserStationEntity? =
        try {
            withContext(ioDispatcher) { stationDao.getRemoteStation(stationId) }
                ?.also { rememberedMeta[stationId] = it }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Timber.w(e, "getRemoteStation failed. stationId: $stationId")
            null
        }

    override fun getRecentStations(limit: Int): Flow<List<Station>> {
        // 조인에서 빠지는 id 가 섞여 있어도 limit 개를 채우도록 조인 뒤에 자른다.
        return stationDao.getRecentStationsWithMeta().map { rows ->
            rows.mapNotNull { stationOf(it.stationId, it.meta) }.take(limit)
        }
    }

    override suspend fun markPlayed(stationId: String) {
        val playedAtMillis = Clock.System.now().toEpochMilliseconds()
        val meta = if (stationId.isRadioBrowserStationId()) {
            // 메타를 못 얻은 해외 스테이션은 기록하지 않는다(메타 없는 행은 목록에서 버려진다).
            metaOf(stationId) ?: return
        } else {
            null
        }
        if (meta != null) {
            stationDao.recordPlayedWithMeta(meta, playedAtMillis, StationRepository.MAX_RECENT_STATIONS)
        } else {
            stationDao.recordPlayed(
                recent = RecentStationEntity(stationId = stationId, playedAtMillis = playedAtMillis),
                keep = StationRepository.MAX_RECENT_STATIONS,
            )
        }
    }

    /**
     * 저장된 id 를 카탈로그와 조인한다. 카탈로그가 먼저이고, 없으면 해외 메타로 만든다. 카탈로그에서 빠진(종료·개편된)
     * 스테이션이거나 메타가 정리된 id 는 재생할 방법이 없으므로 에러로 올리지 않고 조용히 뺀다.
     */
    private fun stationOf(stationId: String, meta: RadioBrowserStationEntity?): Station? {
        meta?.let { rememberedMeta[stationId] = it }
        return stationsById[stationId]?.station ?: meta?.toStation()
    }

    private fun RemoteStation.toEntity() = RadioBrowserStationEntity(
        stationId = station.id,
        uuid = uuid,
        name = station.name,
        region = station.region,
        language = language,
        isHls = isHls,
        savedAtMillis = now(),
    )

    private fun RadioBrowserStationEntity.toStation() = Station(
        id = stationId,
        name = name,
        broadcaster = "",
        kind = StationKind.RADIO,
        region = region,
    )

    override suspend fun getStation(stationId: String): Station? {
        stationsById[stationId]?.let { return it.station }
        if (!stationId.isRadioBrowserStationId()) return null
        knownRemote[stationId]?.let { return it.station }
        return rememberedMeta[stationId]?.toStation() ?: roomMetaOf(stationId)?.toStation()
    }

    override suspend fun resolveStreamUrl(
        stationId: String,
        excluding: Set<String>,
    ): StationStream? = withContext(ioDispatcher) {
        if (stationId.isRadioBrowserStationId()) return@withContext resolveRemoteStreamUrl(stationId, excluding)
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

    /**
     * 해외 스테이션의 주소. `/json/url` 이 준 https 주소를 먼저 쓰고, 실패하거나 http 이거나 이미 실패한 주소면
     * 목록에서 받아 둔 `url_resolved`(https 만 남긴 것)로 간다. 둘 다 안 되면 null.
     * https 가 아닌 주소는 cleartext 허용 호스트가 아니라 재생 시점에 조용히 실패하므로 아예 올리지 않는다.
     */
    private suspend fun resolveRemoteStreamUrl(stationId: String, excluding: Set<String>): StationStream? {
        val known = knownRemote[stationId]
        val resolved = withTimeoutOrNull(RESOLVE_TIMEOUT_MILLIS) {
            try {
                remoteDataSource.resolveStreamUrl(stationId.radioBrowserUuid())
                    ?.takeIf { it.startsWith("https://", ignoreCase = true) && it !in excluding }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.w(e, "resolveStreamUrl failed. stationId: $stationId")
                null
            }
        }
        val url = resolved ?: known?.urlResolved?.takeIf { it !in excluding } ?: return null
        val isHls = known?.isHls
            ?: rememberedMeta[stationId]?.isHls
            ?: roomMetaOf(stationId)?.isHls
            ?: false
        return StationStream(
            url = url,
            isHls = isHls || url.substringBefore('?').endsWith(".m3u8", ignoreCase = true),
        )
    }

    private class CachedDetails(val details: StationDetails?, val fetchedAtMillis: Long)

    // 메모리 캐시만 둔다. 부가 정보라 앱을 다시 켜면 다시 받아도 되고, Room 스키마를 늘릴 값어치가 없다.
    private val detailsCache = mutableMapOf<String, CachedDetails>()
    private val detailsMutex = Mutex()

    override suspend fun getStationDetails(stationId: String): StationDetails? = withContext(ioDispatcher) {
        val uuids = stationsById[stationId]?.radioBrowserIds
            ?: stationId.takeIf { it.isRadioBrowserStationId() }?.let { listOf(it.radioBrowserUuid()) }
            ?: return@withContext null

        // 같은 스테이션을 동시에 부르면 한 번만 받도록 락 안에서 확인하고 받는다. 상세는 부가 정보라
        // 직렬화돼도 상관없다.
        detailsMutex.withLock {
            detailsCache[stationId]
                ?.takeIf { now() - it.fetchedAtMillis < DETAILS_TTL_MILLIS }
                ?.let { return@withLock it.details }

            val fetched = withTimeoutOrNull(DETAILS_TIMEOUT_MILLIS) {
                try {
                    val responses = remoteDataSource.getStationDetails(uuids)
                    Fetched(mergeStationDetails(uuids, responses))
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
    private val scheduleCache = ConcurrentHashMap<String, CachedSchedule>()

    // 스테이션별 락. 전역 락이면 느린 방송사 하나가 다른 스테이션의 갱신까지 막는다.
    private val scheduleMutexes = ConcurrentHashMap<String, Mutex>()

    override fun observeOnAir(
        stationId: String,
        includeSong: Boolean,
        refresh: OnAirRefresh,
    ): Flow<OnAirProgram?> {
        val source = stationsById[stationId]?.onAirSource ?: return flowOf(null)
        val programs = onAirPrograms(stationId, source, refresh)
        // 곡은 프로그램 경계와 상관없이 바뀌므로 따로 폴링해 얹는다. 곡을 주는 방송사는 MBC 뿐이다.
        val flow = if (includeSong && source is OnAirSource.Mbc) {
            // 곡 응답을 기다리느라 프로그램이 늦지 않게 null 로 시작한다.
            combine(programs, onAirSongs(source).onStart { emit(null) }) { program, log ->
                // "마지막 기록 곡" 이라 이 프로그램이 시작하기 전에 기록된 곡은 앞 프로그램의 것이다.
                val song = log?.takeIf { (it.loggedAtMillis ?: return@takeIf false) >= (program?.startMillis ?: Long.MAX_VALUE) }
                program?.copy(song = song?.text)
            }
        } else {
            programs
        }
        return flow.distinctUntilChanged()
    }

    private fun onAirPrograms(
        stationId: String,
        source: OnAirSource,
        refresh: OnAirRefresh,
    ): Flow<OnAirProgram?> = flow {
        val boundaryOnly = refresh == OnAirRefresh.Boundary
        while (true) {
            val schedule = loadSchedule(stationId, source, allowStale = boundaryOnly)
            val nowMillis = now()
            val current = schedule?.currentAt(nowMillis)
            emit(current)

            // 임시 편성으로 끝 시각이 어긋나도 따라가도록 끝 시각까지 가더라도 SCHEDULE_TTL 이상은 자지 않는다.
            // 현재 항목이 없으면 다음 항목의 시작까지, 다음도 없으면 재시도 간격만큼 기다린다.
            val untilChange = when {
                schedule == null -> ONAIR_FAILURE_RETRY_MILLIS
                current != null -> (current.endMillis ?: Long.MAX_VALUE) - nowMillis
                else -> (schedule.nextStartAfter(nowMillis) ?: (nowMillis + ONAIR_FAILURE_RETRY_MILLIS)) - nowMillis
            }
            // Boundary 는 TTL 로 깨지 않고 프로그램 끝까지 잔다. 하루를 넘기지는 않는다(날짜가 바뀌면 편성표를 새로 받는다).
            val maxWait = if (boundaryOnly) DAY_MILLIS else SCHEDULE_TTL_MILLIS
            delay(untilChange.coerceIn(ONAIR_MIN_WAIT_MILLIS, maxWait))
        }
    }

    override fun observeSchedule(stationId: String): Flow<StationSchedule> {
        val source = stationsById[stationId]?.onAirSource
            ?: return flowOf(StationSchedule(dayStartOf(now()), emptyList()))
        return flow {
            while (true) {
                val schedule = loadSchedule(stationId, source)
                val nowMillis = now()
                emit(StationSchedule(dayStartOf(nowMillis), schedule?.windowAt(nowMillis).orEmpty()))
                // 편성표는 TTL 마다 다시 받으므로 그 주기로 다시 읽되, 자정에는 맞춰 깬다. 날짜가 바뀌면 "오늘" 기준과
                // "내일" 구분이 한꺼번에 밀리는데 5분 고정이면 자정 직후 그만큼 어긋난다.
                val untilMidnight = dayStartOf(nowMillis) + DAY_MILLIS - nowMillis
                val wait = if (schedule == null) ONAIR_FAILURE_RETRY_MILLIS else SCHEDULE_TTL_MILLIS
                delay(minOf(wait, untilMidnight).coerceAtLeast(ONAIR_MIN_WAIT_MILLIS))
            }
        }.distinctUntilChanged()
    }

    private class CachedSong(val log: OnAirSongLog?, val fetchedAtMillis: Long)

    private val songCache = ConcurrentHashMap<String, CachedSong>()
    private val songMutexes = ConcurrentHashMap<String, Mutex>()

    /**
     * 곡 로그를 [SONG_POLL_MILLIS] 마다 읽어 발행한다. 기록된 지 [SONG_MAX_AGE_MILLIS] 가 지난 곡은 버린다 — 요청이
     * 계속 실패하는 동안 직전 곡을 무기한 보이지 않으려는 것이고, 정상 응답이어도 같은 기준이다.
     */
    private fun onAirSongs(source: OnAirSource.Mbc): Flow<OnAirSongLog?> = flow {
        while (true) {
            emit(loadSong(source)?.takeIf { log -> log.loggedAtMillis?.let { now() - it <= SONG_MAX_AGE_MILLIS } == true })
            delay(SONG_POLL_MILLIS)
        }
    }

    /**
     * 같은 채널을 여러 곳(시트·미니바)에서 구독해도 [SONG_CACHE_MILLIS] 안에는 한 번만 부른다. 실패하면 마지막으로 받은
     * 값을 돌려주되(만료는 호출한 쪽이 건다) 캐시 시각은 갱신하지 않아 다음 호출에서 다시 시도한다.
     */
    private suspend fun loadSong(source: OnAirSource.Mbc): OnAirSongLog? {
        return songMutexes.getOrPut(source.channel) { Mutex() }.withLock {
            val nowMillis = now()
            val cached = songCache[source.channel]
            if (cached != null && nowMillis - cached.fetchedAtMillis < SONG_CACHE_MILLIS) return@withLock cached.log
            try {
                onAirDataSource.currentSong(source).also { songCache[source.channel] = CachedSong(it, nowMillis) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.w(e, "currentSong failed. source: $source")
                cached?.log
            }
        }
    }

    /**
     * 편성표를 캐시에서 주거나 받아 온다. 날짜가 바뀌었거나 [SCHEDULE_TTL_MILLIS] 가 지났거나([allowStale] 이면 TTL 은 보지 않는다), 캐시에 지금 시각의
     * 항목이 없는데 마지막으로 받은 지 [SCHEDULE_REFETCH_MIN_MILLIS] 가 지났으면 다시 받는다. 다시 받다 실패해도
     * 같은 날짜의 캐시가 있으면 그것을 쓰고, 어제 캐시라도 지금 시각의 프로그램이 걸려 있으면 그것을 쓴다(실패 자체는 캐시하지 않으므로 다음에 다시 시도한다). 그도 없으면 null.
     */
    private suspend fun loadSchedule(
        stationId: String,
        source: OnAirSource,
        allowStale: Boolean = false,
    ): List<OnAirProgram>? {
        return scheduleMutexes.getOrPut(stationId) { Mutex() }.withLock {
            val nowMillis = now()
            val dateKey = Instant.ofEpochMilli(nowMillis).atZone(SEOUL).toLocalDate().toString()
            val previous = scheduleCache[stationId]
            val cached = previous?.takeIf { it.dateKey == dateKey }
            if (cached != null) {
                val age = nowMillis - cached.fetchedAtMillis
                // allowStale 이면 TTL 이 지나도 지금 프로그램이 캐시에 있는 한 그대로 쓴다.
                val fresh = (allowStale || age < SCHEDULE_TTL_MILLIS) &&
                    (cached.programs.currentAt(nowMillis) != null || age < SCHEDULE_REFETCH_MIN_MILLIS)
                if (fresh) return@withLock cached.programs
            }
            try {
                // 조각·필러를 다듬은 뒤 캐시한다. 편성 목록과 현재 프로그램이 같은 목록을 본다.
                onAirDataSource.schedule(source, nowMillis).normalizeSchedule().also {
                    scheduleCache[stationId] = CachedSchedule(it, dateKey, nowMillis)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.w(e, "onAir schedule failed. stationId: $stationId, source: $source")
                // 자정 재조회가 실패해도 어제 받은 편성에 지금 시각의 프로그램이 걸려 있으면(자정을 넘기는 방송) 그것을 쓴다.
                cached?.programs ?: previous?.programs?.takeIf { it.currentAt(nowMillis) != null }
            }
        }
    }

    private fun List<OnAirProgram>.currentAt(nowMillis: Long): OnAirProgram? =
        filter { (it.startMillis ?: return@filter false) <= nowMillis && nowMillis < (it.endMillis ?: return@filter false) }
            .maxByOrNull { it.startMillis ?: 0L }

    private fun dayStartOf(nowMillis: Long): Long =
        Instant.ofEpochMilli(nowMillis).atZone(SEOUL).toLocalDate().atStartOfDay(SEOUL).toInstant().toEpochMilli()

    /**
     * [nowMillis] 가 속한 날 00:00 부터 [SCHEDULE_WINDOW_MILLIS] 안에 걸리는 프로그램. 시각을 모르는 항목은 뺀다. 시작순.
     * 끝은 내일 05:00 — KBS 의 방송일(05:00~29:00)과 같은 선이다.
     */
    private fun List<OnAirProgram>.windowAt(nowMillis: Long): List<OnAirProgram> {
        val from = dayStartOf(nowMillis)
        val to = from + SCHEDULE_WINDOW_MILLIS
        return filter { (it.startMillis ?: return@filter false) < to && (it.endMillis ?: return@filter false) > from }
            .sortedBy { it.startMillis }
    }

    private fun List<OnAirProgram>.nextStartAfter(nowMillis: Long): Long? =
        mapNotNull { it.startMillis }.filter { it > nowMillis }.minOrNull()

    private companion object {
        const val CURATED_COUNTRY = "KR"
        val COUNTRY_CODE = Regex("[A-Z]{2}")
        const val LISTING_TTL_MILLIS = 6 * 60 * 60 * 1000L
        const val LISTING_TIMEOUT_MILLIS = 10_000L
        const val RESOLVE_TIMEOUT_MILLIS = 15_000L
        const val BROADCASTER_TIMEOUT_MILLIS = 5_000L
        const val DETAILS_TIMEOUT_MILLIS = 10_000L
        const val DETAILS_TTL_MILLIS = 24 * 60 * 60 * 1000L
        const val DAY_MILLIS = 24 * 60 * 60 * 1000L
        const val SCHEDULE_WINDOW_MILLIS = 29 * 60 * 60 * 1000L
        const val SCHEDULE_TTL_MILLIS = 5 * 60 * 1000L
        const val SCHEDULE_REFETCH_MIN_MILLIS = 30_000L
        const val ONAIR_FAILURE_RETRY_MILLIS = 60_000L // 실패·편성 공백 뒤 재시도
        const val ONAIR_MIN_WAIT_MILLIS = 1_000L
        const val SONG_POLL_MILLIS = 45_000L
        const val SONG_CACHE_MILLIS = 30_000L
        const val SONG_MAX_AGE_MILLIS = 10 * 60 * 1000L
        val SEOUL: ZoneId = ZoneId.of("Asia/Seoul")
    }
}
