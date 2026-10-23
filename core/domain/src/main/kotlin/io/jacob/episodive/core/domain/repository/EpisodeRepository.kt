package io.jacob.episodive.core.domain.repository

import androidx.paging.PagingData
import io.jacob.episodive.core.model.Category
import io.jacob.episodive.core.model.Chapter
import io.jacob.episodive.core.model.Episode
import kotlinx.coroutines.flow.Flow
import kotlin.time.Duration
import kotlin.time.Instant

interface EpisodeRepository {
    suspend fun upsertEpisode(episode: Episode)

    fun searchEpisodesByPerson(
        person: String,
        max: Int,
    ): Flow<List<Episode>>

    fun searchEpisodesByPersonPaging(
        person: String,
    ): Flow<PagingData<Episode>>

    fun getEpisodesByFeedId(
        feedId: Long,
        max: Int,
    ): Flow<List<Episode>>

    fun getEpisodesByFeedIdPaging(
        feedId: Long,
    ): Flow<PagingData<Episode>>

    /**
     * [getEpisodesByFeedIdPaging] 의 캐시를 만료와 무관하게 원격에서 다시 받는다. 끝날 때까지 기다리고,
     * 실패하면 [io.jacob.episodive.core.model.DataErrorException] 을 던진다(캐시는 그대로 남는다).
     */
    suspend fun refreshEpisodesByFeedId(feedId: Long)

    fun getEpisodesByFeedUrl(
        feedUrl: String,
        max: Int,
    ): Flow<List<Episode>>

    fun getEpisodesByFeedUrlPaging(
        feedUrl: String,
    ): Flow<PagingData<Episode>>

    fun getEpisodesByPodcastGuid(
        guid: String,
        max: Int,
    ): Flow<List<Episode>>

    fun getEpisodesByPodcastGuidPaging(
        guid: String,
    ): Flow<PagingData<Episode>>

    fun getLiveEpisodes(max: Int): Flow<List<Episode>>

    /**
     * 라이브 에피소드를 전체 목록용으로 받아 페이징한다.
     *
     * 미리보기와 별도 캐시 그룹을 쓴다. 한 그룹을 공유하면 먼저 캐시를 채운 쪽의 개수에
     * 갇히므로, 여기서 받은 [max] 가 그대로 전체 목록의 상한이 된다.
     */
    fun getLiveEpisodesPaging(max: Int): Flow<PagingData<Episode>>

    fun getRandomEpisodes(
        max: Int,
        language: String? = null,
        includeCategories: List<Category> = emptyList(),
    ): Flow<List<Episode>>

    /** 랜덤 에피소드의 전체 목록판. 캐시 분리는 [getLiveEpisodesPaging] 과 같다. */
    fun getRandomEpisodesPaging(
        max: Int,
        language: String? = null,
        includeCategories: List<Category> = emptyList(),
    ): Flow<PagingData<Episode>>

    fun getRecentEpisodes(max: Int): Flow<List<Episode>>

    fun getSoundbiteEpisodesPaging(max: Int): Flow<PagingData<Episode>>

    fun getEpisodeById(id: Long): Flow<Episode?>

    /**
     * 원격에서 에피소드 하나를 받아 온다. 실패하거나 없으면 null.
     *
     * [getEpisodeById] 는 로컬 DB 만 보므로, 공유받은 링크처럼 이 기기가 한 번도 만난 적 없는
     * 에피소드는 영영 null 이다. 그 경우의 마지막 수단이다.
     */
    suspend fun fetchEpisodeById(id: Long): Episode?

    /**
     * 목록/단건 조회 모두 fulltext 없이 받아 description 이 짧게 잘려 있다. 재생 중인
     * 에피소드가 바뀌는 시점에 호출해 fulltext=true 로 단건을 다시 받아 description 만 보강한다.
     * 실패해도 예외를 던지지 않는다 — 보강은 부가 기능이라 실패해도 기존 캐시로 계속 보여준다.
     */
    suspend fun refreshEpisodeDescription(id: Long)

    fun getEpisodesByIds(ids: List<Long>): Flow<List<Episode>>

    fun getLikedEpisodes(query: String? = null, max: Int): Flow<List<Episode>>

    fun getLikedEpisodesPaging(query: String? = null): Flow<PagingData<Episode>>

    fun getPlayedEpisodes(
        isCompleted: Boolean? = null,
        query: String? = null,
        max: Int,
    ): Flow<List<Episode>>

    fun getPlayedEpisodesPaging(
        isCompleted: Boolean? = null,
        query: String? = null,
    ): Flow<PagingData<Episode>>

    fun isLikedEpisode(episode: Episode): Flow<Boolean>

    suspend fun toggleLikedEpisode(episode: Episode): Boolean

    suspend fun updatePlayed(id: Long, position: Duration, isCompleted: Boolean)

    suspend fun updateEpisodeDuration(id: Long, duration: Duration)

    suspend fun replaceEpisodes(episodes: List<Episode>, groupKey: String)

    suspend fun fetchChapters(url: String): List<Chapter>

    suspend fun getEpisodesByGroupKey(groupKey: String): List<Episode>

    fun getSavedEpisodes(query: String? = null, max: Int): Flow<List<Episode>>

    fun getSavedEpisodesPaging(query: String? = null): Flow<PagingData<Episode>>

    suspend fun toggleSavedEpisode(episode: Episode): Boolean

    suspend fun removeSavedEpisode(id: Long)

    suspend fun getLatestEpisodeDatePublished(feedId: Long): Instant?

    suspend fun fetchAndSaveNewEpisodes(feedId: Long, since: Instant): List<Episode>
}
