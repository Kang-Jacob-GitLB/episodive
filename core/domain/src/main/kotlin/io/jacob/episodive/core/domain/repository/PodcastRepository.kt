package io.jacob.episodive.core.domain.repository

import androidx.paging.PagingData
import io.jacob.episodive.core.model.Category
import io.jacob.episodive.core.model.Channel
import io.jacob.episodive.core.model.Podcast
import kotlinx.coroutines.flow.Flow
import kotlin.time.Instant

interface PodcastRepository {
    fun searchPodcasts(
        query: String,
        max: Int,
    ): Flow<List<Podcast>>

    fun getPodcastByFeedId(feedId: Long): Flow<Podcast?>

    /**
     * [getPodcastByFeedId] 의 캐시를 만료와 무관하게 원격에서 다시 받는다. 끝날 때까지 기다리고,
     * 실패하면 [io.jacob.episodive.core.model.DataErrorException] 을 던진다(캐시는 그대로 남는다).
     */
    suspend fun refreshPodcastByFeedId(feedId: Long)

    fun getPodcastByFeedUrl(feedUrl: String): Flow<Podcast?>

    fun getPodcastByGuid(guid: String): Flow<Podcast?>

    fun getPodcastsByMedium(
        medium: String,
        max: Int,
    ): Flow<List<Podcast>>

    fun getPodcastsByMediumPaging(medium: String): Flow<PagingData<Podcast>>

    fun getPodcastsByChannel(channel: Channel): Flow<List<Podcast>>

    fun getPodcastsByChannelPaging(channel: Channel): Flow<PagingData<Podcast>>

    fun getTrendingPodcasts(
        max: Int,
        language: String? = null,
        includeCategories: List<Category> = emptyList(),
    ): Flow<List<Podcast>>

    /**
     * 같은 조건의 트렌딩을 전체 목록용으로 받아 페이징한다.
     *
     * 미리보기와 별도 캐시 그룹을 쓴다. 한 그룹을 공유하면 먼저 캐시를 채운 쪽의 개수에
     * 갇히므로, 여기서 받은 [max] 가 그대로 전체 목록의 상한이 된다.
     */
    fun getTrendingPodcastsPaging(
        max: Int,
        language: String? = null,
        includeCategories: List<Category> = emptyList(),
    ): Flow<PagingData<Podcast>>

    fun getRecentPodcasts(
        max: Int,
        language: String? = null,
        includeCategories: List<Category> = emptyList(),
    ): Flow<List<Podcast>>

    /** 최근 발행 팟캐스트의 전체 목록판. 캐시 분리는 [getTrendingPodcastsPaging] 과 같다. */
    fun getRecentPodcastsPaging(
        max: Int,
        language: String? = null,
        includeCategories: List<Category> = emptyList(),
    ): Flow<PagingData<Podcast>>

    fun getRecentNewPodcasts(
        max: Int,
    ): Flow<List<Podcast>>

    fun getRecommendedPodcasts(
        max: Int,
        language: String? = null,
        includeCategories: List<Category> = emptyList(),
    ): Flow<List<Podcast>>

    fun getRecommendedPodcastsPaging(
        max: Int,
        language: String? = null,
        includeCategories: List<Category> = emptyList(),
    ): Flow<PagingData<Podcast>>

    fun getFollowedPodcasts(
        query: String? = null,
        max: Int,
    ): Flow<List<Podcast>>

    fun getFollowedPodcastsPaging(query: String? = null): Flow<PagingData<Podcast>>

    suspend fun toggleFollowed(id: Long): Boolean

    /** OPML 내보내기용 전량 조회. 페이징 없이 한 번에 List 로 받는다. */
    suspend fun getFollowedPodcastsOnce(): List<Podcast>

    /**
     * [toggleFollowed] 와 달리 **멱등한 추가**다. 이미 팔로우 중이면 아무것도 바꾸지 않고
     * false 를 돌려준다 — 절대 해제하지 않는다. OPML 가져오기가 토글을 쓰면 이미 구독
     * 중인 팟캐스트가 오히려 풀려버리므로 이 메서드가 필요하다.
     *
     * @return 이번 호출로 새로 팔로우가 추가됐으면 true, 이미 팔로우 중이었으면 false.
     */
    suspend fun followPodcast(id: Long): Boolean

    suspend fun getFollowedPodcastsToSync(): Map<Long, Instant>
}