package io.jacob.episodive.core.data.repository

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import io.jacob.episodive.core.data.util.paging.PagingDefaults
import io.jacob.episodive.core.data.util.paging.SoundbiteEpisodePagingSource
import io.jacob.episodive.core.data.util.query.EpisodeQuery
import io.jacob.episodive.core.data.util.query.QueryScope
import io.jacob.episodive.core.data.util.updater.EpisodeRemoteUpdater
import io.jacob.episodive.core.database.datasource.EpisodeLocalDataSource
import io.jacob.episodive.core.database.datasource.SoundbiteLocalDataSource
import io.jacob.episodive.core.database.mapper.toEpisode
import io.jacob.episodive.core.database.mapper.toEpisodeEntities
import io.jacob.episodive.core.database.mapper.toEpisodeEntity
import io.jacob.episodive.core.database.mapper.toEpisodes
import io.jacob.episodive.core.database.model.PlayedEpisodeEntity
import io.jacob.episodive.core.domain.repository.EpisodeRepository
import io.jacob.episodive.core.model.Category
import io.jacob.episodive.core.model.Chapter
import io.jacob.episodive.core.model.Episode
import io.jacob.episodive.core.network.datasource.ChapterRemoteDataSource
import io.jacob.episodive.core.network.datasource.EpisodeRemoteDataSource
import io.jacob.episodive.core.network.datasource.SoundbiteRemoteDataSource
import io.jacob.episodive.core.network.mapper.toEpisode as responseToEpisode
import io.jacob.episodive.core.network.mapper.toEpisodes
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import timber.log.Timber
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Instant

class EpisodeRepositoryImpl @Inject constructor(
    private val episodeLocalDataSource: EpisodeLocalDataSource,
    private val episodeRemoteDataSource: EpisodeRemoteDataSource,
    private val chapterRemoteDataSource: ChapterRemoteDataSource,
    private val soundbiteLocalDataSource: SoundbiteLocalDataSource,
    private val soundbiteRemoteDataSource: SoundbiteRemoteDataSource,
    private val remoteUpdater: EpisodeRemoteUpdater.Factory,
) : EpisodeRepository {
    private val config = PagingDefaults.DEFAULT_CONFIG

    // refreshEpisodeDescription 이 지금 진행 중인 에피소드 id 집합. "보강을 끝냈다"는 영구
    // 표시가 아니라 "지금 요청 중"이라는 표시다 — 이유는 refreshEpisodeDescription 의 KDoc 참고.
    private val refreshingEpisodeIds: MutableSet<Long> = ConcurrentHashMap.newKeySet()

    override suspend fun upsertEpisode(episode: Episode) {
        episodeLocalDataSource.upsertEpisode(episode.toEpisodeEntity())
    }

    override fun searchEpisodesByPerson(
        person: String,
        max: Int,
    ): Flow<List<Episode>> {
        val query = EpisodeQuery.Person(person)

        return remoteUpdater.create(query)
            .getFlowList(max)
            .map { it.toEpisodes() }
    }

    override fun searchEpisodesByPersonPaging(
        person: String,
    ): Flow<PagingData<Episode>> {
        val query = EpisodeQuery.Person(person)

        return remoteUpdater.create(query)
            .getPagingData(config)
            .map { pagingData ->
                pagingData.map { it.toEpisode() }
            }
    }

    override fun getEpisodesByFeedId(
        feedId: Long,
        max: Int,
    ): Flow<List<Episode>> = flow {
        episodeRemoteDataSource.getEpisodesByFeedId(
            feedId = feedId,
            max = max,
        ).toEpisodes()
            .let { emit(it) }
    }

    override fun getEpisodesByFeedIdPaging(feedId: Long): Flow<PagingData<Episode>> {
        val query = EpisodeQuery.FeedId(feedId)

        return remoteUpdater.create(query)
            .getPagingData(config)
            .map { pagingData ->
                pagingData.map { it.toEpisode() }
            }
    }

    override suspend fun refreshEpisodesByFeedId(feedId: Long) {
        remoteUpdater.create(EpisodeQuery.FeedId(feedId)).forceRefresh()
    }

    override fun getEpisodesByFeedUrl(
        feedUrl: String,
        max: Int,
    ): Flow<List<Episode>> {
        val query = EpisodeQuery.FeedUrl(feedUrl)

        return remoteUpdater.create(query)
            .getFlowList(max)
            .map { it.toEpisodes() }
    }

    override fun getEpisodesByFeedUrlPaging(feedUrl: String): Flow<PagingData<Episode>> {
        val query = EpisodeQuery.FeedUrl(feedUrl)

        return remoteUpdater.create(query)
            .getPagingData(config)
            .map { pagingData ->
                pagingData.map { it.toEpisode() }
            }
    }

    override fun getEpisodesByPodcastGuid(
        guid: String,
        max: Int,
    ): Flow<List<Episode>> {
        val query = EpisodeQuery.PodcastGuid(guid)

        return remoteUpdater.create(query)
            .getFlowList(max)
            .map { it.toEpisodes() }
    }

    override fun getEpisodesByPodcastGuidPaging(guid: String): Flow<PagingData<Episode>> {
        val query = EpisodeQuery.PodcastGuid(guid)

        return remoteUpdater.create(query)
            .getPagingData(config)
            .map { pagingData ->
                pagingData.map { it.toEpisode() }
            }
    }

    override fun getLiveEpisodes(max: Int): Flow<List<Episode>> {
        val query = EpisodeQuery.Live(max = max)

        return remoteUpdater.create(query)
            .getFlowList(max)
            .map { it.toEpisodes() }
    }

    override fun getLiveEpisodesPaging(max: Int): Flow<PagingData<Episode>> {
        val query = EpisodeQuery.Live(max = max, scope = QueryScope.FULL)

        return remoteUpdater.create(query)
            .getPagingData(config)
            .map { pagingData ->
                pagingData.map { it.toEpisode() }
            }
    }

    override fun getRandomEpisodes(
        max: Int,
        language: String?,
        includeCategories: List<Category>,
    ): Flow<List<Episode>> {
        val query = EpisodeQuery.Random(max, language, includeCategories)

        return remoteUpdater.create(query)
            .getFlowList(max)
            .map { it.toEpisodes() }
    }

    override fun getRandomEpisodesPaging(
        max: Int,
        language: String?,
        includeCategories: List<Category>,
    ): Flow<PagingData<Episode>> {
        val query = EpisodeQuery.Random(max, language, includeCategories, QueryScope.FULL)

        return remoteUpdater.create(query)
            .getPagingData(config)
            .map { pagingData ->
                pagingData.map { it.toEpisode() }
            }
    }

    override fun getRecentEpisodes(max: Int): Flow<List<Episode>> {
        val query = EpisodeQuery.Recent(max)

        return remoteUpdater.create(query)
            .getFlowList(max)
            .map { it.toEpisodes() }
    }

    override fun getSoundbiteEpisodesPaging(max: Int): Flow<PagingData<Episode>> {
        return Pager(
            config = PagingConfig(
                pageSize = 5,
                prefetchDistance = 5,
                initialLoadSize = 10,
                enablePlaceholders = false
            ),
            pagingSourceFactory = {
                SoundbiteEpisodePagingSource(
                    database = episodeLocalDataSource.database,
                    episodeLocal = episodeLocalDataSource,
                    episodeRemote = episodeRemoteDataSource,
                    soundbiteLocal = soundbiteLocalDataSource,
                    soundbiteRemote = soundbiteRemoteDataSource,
                    maxSoundbites = max
                )
            }
        ).flow.map { pagingData ->
            pagingData.map { it.toEpisode() }
        }
    }

    override fun getEpisodeById(id: Long): Flow<Episode?> {
        return episodeLocalDataSource.getEpisodeById(id)
            .map { it?.toEpisode() }
    }

    /**
     * 로컬에 심지 않는다. `replaceEpisodes` 는 그룹을 통째로 갈아치우므로 여기서 부르면
     * 사용자가 듣던 재생목록(`GroupKey.PLAYLIST`)이 날아가고, 어차피 이 값을 받아 재생을 거는
     * `PlayEpisodeUseCase` 가 곧바로 같은 그룹에 심는다.
     *
     * 실패를 예외로 올리지 않는 것도 의도한 선택이다. 호출부는 "로컬에 없으면 원격도 본다"는
     * 폴백 자리라, 없는 것과 못 가져온 것을 똑같이 다루면 된다. 취소만은 다시 던진다.
     */
    override suspend fun fetchEpisodeById(id: Long): Episode? = try {
        episodeRemoteDataSource.getEpisodeById(id, fulltext = true)?.responseToEpisode()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Timber.w(e, "에피소드 단건 조회에 실패했다 (id=$id)")
        null
    }

    /**
     * 목록/단건 조회 모두 fulltext 를 켜지 않으므로 description 이 짧게 잘려 있다. 재생 중인
     * 에피소드가 바뀔 때(PlayerViewModel)마다 이 메서드를 호출해 fulltext=true 단건 재조회로
     * description 만 보강한다. 호출 지점은 이 클래스 밖에서 정한다.
     *
     * 보강은 부가 기능이라 실패해도 화면에는 영향이 없어야 한다 — 이미 캐시된 짧은 설명이라도
     * 계속 보여야 하므로 여기서 예외를 올리지 않는다(RemoteUpdater 의 "캐시 없으면 throw" 분기는
     * 여기엔 없다). 취소만은 다시 던져 코루틴 취소가 전파되게 한다.
     *
     * [refreshingEpisodeIds] 는 "이 에피소드는 보강을 끝냈다"는 영구 표시가 아니라 "지금 요청
     * 중"이라는 표시다. `EpisodeDao.replaceEpisodes()`(→ upsertEpisodesWithGroup → upsertEpisodes)
     * 는 `@Upsert` 라 행 전체를 교체한다 — `EpisodeRemoteUpdater` 가 목록 캐시를 갱신할 때(TTL
     * 10분~1일) 그 목록에 포함된 에피소드의 description 이 원격의 잘린 값으로 되돌아갈 수 있다.
     * 영구 표시로 남겨두면 그 뒤로는 앱을 재시작하기 전까지 잘린 설명이 고정된다. 그래서 요청이
     * 끝나면(성공하든 실패하든) 표시를 지우고, 다음에 같은 에피소드를 다시 열면 원격을 다시 한 번
     * 친다 — 대신 PlayerViewModel 쪽 `distinctUntilChanged` 가 연속 중복 방출을 막아 주므로
     * 실제 추가 호출은 에피소드 전환당 1회(응답 약 4KB)뿐이다.
     * `add()` 가 원자적이라 동시 진입 중 먼저 성공한 호출만 실제로 원격을 친다.
     */
    override suspend fun refreshEpisodeDescription(id: Long) {
        if (!refreshingEpisodeIds.add(id)) return

        try {
            val fullDescription = episodeRemoteDataSource.getEpisodeById(id, fulltext = true)?.description
            if (fullDescription.isNullOrEmpty()) return

            val currentDescription = episodeLocalDataSource.getEpisodeDescription(id)
            // 원격이 기존보다 짧거나 같으면 쓰지 않는다.
            if (currentDescription == null || fullDescription.length > currentDescription.length) {
                episodeLocalDataSource.updateEpisodeDescription(id, fullDescription)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Timber.w(e, "에피소드 설명 보강에 실패했다 (id=$id)")
        } finally {
            refreshingEpisodeIds.remove(id)
        }
    }

    override fun getEpisodesByIds(ids: List<Long>): Flow<List<Episode>> {
        return episodeLocalDataSource.getEpisodesByIds(ids)
            .map { it.toEpisodes() }
    }

    override fun getLikedEpisodes(query: String?, max: Int): Flow<List<Episode>> {
        return episodeLocalDataSource.getLikedEpisodes(
            query = query,
            limit = max,
        ).map { it.toEpisodes() }
    }

    override fun getLikedEpisodesPaging(query: String?): Flow<PagingData<Episode>> {
        return Pager(
            config = config,
            pagingSourceFactory = { episodeLocalDataSource.getLikedEpisodesPaging(query) }
        ).flow.map { pagingData ->
            pagingData.map { it.toEpisode() }
        }
    }

    override fun getPlayedEpisodes(
        isCompleted: Boolean?,
        query: String?,
        max: Int,
    ): Flow<List<Episode>> {
        return episodeLocalDataSource.getPlayedEpisodes(
            isCompleted = isCompleted,
            query = query,
            limit = max,
        ).map { it.toEpisodes() }
    }

    override fun getPlayedEpisodesPaging(
        isCompleted: Boolean?,
        query: String?,
    ): Flow<PagingData<Episode>> {
        return Pager(
            config = config,
            pagingSourceFactory = {
                episodeLocalDataSource.getPlayedEpisodesPaging(
                    isCompleted = isCompleted,
                    query = query,
                )
            }
        ).flow.map { pagingData ->
            pagingData.map { it.toEpisode() }
        }
    }

    override fun isLikedEpisode(episode: Episode): Flow<Boolean> {
        return episodeLocalDataSource.isLikedEpisode(episode.toEpisodeEntity())
    }

    override suspend fun toggleLikedEpisode(episode: Episode): Boolean {
        return episodeLocalDataSource.toggleLikedEpisode(episode.toEpisodeEntity())
    }

    override suspend fun updatePlayed(
        id: Long,
        position: Duration,
        isCompleted: Boolean,
    ) {
        episodeLocalDataSource.updatePlayedEpisode(
            PlayedEpisodeEntity(
                id = id,
                playedAt = Clock.System.now(),
                position = position,
                isCompleted = isCompleted,
            )
        )
    }

    override suspend fun updateEpisodeDuration(id: Long, duration: Duration) {
        episodeLocalDataSource.updateEpisodeDuration(id, duration)
    }

    override suspend fun replaceEpisodes(episodes: List<Episode>, groupKey: String) {
        episodeLocalDataSource.replaceEpisodes(episodes.toEpisodeEntities(), groupKey)
    }

    override suspend fun fetchChapters(url: String): List<Chapter> {
        return chapterRemoteDataSource.fetchChapters(url)
    }

    override suspend fun getEpisodesByGroupKey(groupKey: String): List<Episode> {
        return episodeLocalDataSource.getEpisodesByGroupKey(groupKey, Int.MAX_VALUE)
            .first()
            .toEpisodes()
    }

    override fun getSavedEpisodes(query: String?, max: Int): Flow<List<Episode>> {
        return episodeLocalDataSource.getSavedEpisodes(
            query = query,
            limit = max,
        ).map { it.toEpisodes() }
    }

    override fun getSavedEpisodesPaging(query: String?): Flow<PagingData<Episode>> {
        return Pager(
            config = config,
            pagingSourceFactory = { episodeLocalDataSource.getSavedEpisodesPaging(query) }
        ).flow.map { pagingData ->
            pagingData.map { it.toEpisode() }
        }
    }

    override suspend fun toggleSavedEpisode(episode: Episode): Boolean {
        val filePath = "${episode.feedId}/${episode.id}.${episode.enclosureType.substringAfterLast("/", "mp3")}"
        return episodeLocalDataSource.toggleSavedEpisode(episode.toEpisodeEntity(), filePath)
    }

    override suspend fun removeSavedEpisode(id: Long) {
        episodeLocalDataSource.removeSavedEpisode(id)
    }

    override suspend fun getLatestEpisodeDatePublished(feedId: Long): Instant? {
        return episodeLocalDataSource.getLatestEpisodeDatePublished(feedId)
    }

    override suspend fun fetchAndSaveNewEpisodes(feedId: Long, since: Instant): List<Episode> {
        val responses = episodeRemoteDataSource.getEpisodesByFeedId(
            feedId = feedId,
            since = since.epochSeconds,
        )
        val episodes = responses.toEpisodes()
        episodeLocalDataSource.upsertEpisodes(episodes.toEpisodeEntities())
        // since 는 보유한 최신 에피소드의 발행 시각이며, API 의 since 경계가 inclusive 일 경우
        // 이미 가진 에피소드가 다시 반환될 수 있다. 알림 중복 발송을 막기 위해
        // 실제로 since 이후에 발행된 새 에피소드만 반환한다.
        return episodes.filter { it.datePublished > since }
    }
}
