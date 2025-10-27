package io.jacob.episodive.core.domain.usecase.podcast

import io.jacob.episodive.core.domain.repository.FeedRepository
import io.jacob.episodive.core.domain.repository.PodcastRepository
import io.jacob.episodive.core.domain.repository.UserRepository
import io.jacob.episodive.core.model.Podcast
import io.jacob.episodive.core.model.mapper.toFeedsFromRecent
import io.jacob.episodive.core.model.mapper.toFeedsFromTrending
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject

class GetRecommendedPodcastsUseCase @Inject constructor(
    private val userRepository: UserRepository,
    private val feedRepository: FeedRepository,
    private val podcastRepository: PodcastRepository,
) {
    operator fun invoke(): Flow<List<Podcast>> {
        return userRepository.getUserData().flatMapLatest { userData ->
            if (userData.categories.isEmpty()) {
                flowOf(emptyList())
            } else {
                combine(
                    feedRepository.getTrendingFeeds(
                        language = userData.language,
                        includeCategories = userData.categories
                    ),
                    feedRepository.getRecentFeeds(
                        language = userData.language,
                        includeCategories = userData.categories
                    ),
                ) { trending, recent ->
                    (trending.toFeedsFromTrending() + recent.toFeedsFromRecent())
                        .distinctBy { it.id }
                        .sortedByDescending { it.newestItemPublishTime }
                }.flatMapLatest { feeds ->
                    flow {
                        val podcasts = mutableListOf<Podcast>()
                        feeds.chunked(5).forEach { chunk ->
                            // Fetch 5 podcasts in parallel while maintaining order
                            val chunkPodcasts = coroutineScope {
                                chunk.map { feed ->
                                    async {
                                        podcastRepository.getPodcastByFeedId(feed.id).first()
                                    }
                                }.awaitAll().filterNotNull()
                            }
                            podcasts.addAll(chunkPodcasts)
                            emit(podcasts.toList())
                        }
                    }
                }
            }
        }
    }
}