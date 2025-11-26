package io.jacob.episodive.core.domain.usecase.podcast

import io.jacob.episodive.core.domain.repository.FeedRepository
import io.jacob.episodive.core.domain.repository.PodcastRepository
import io.jacob.episodive.core.domain.repository.UserRepository
import io.jacob.episodive.core.model.Podcast
import io.jacob.episodive.core.model.mapper.toFeedsFromRecent
import io.jacob.episodive.core.model.mapper.toFeedsFromTrending
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
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
                    if (feeds.isEmpty()) {
                        return@flatMapLatest flowOf(emptyList())
                    }

                    channelFlow {
                        // Create flows for each podcast that continuously observe changes
                        val allPodcastFlows = mutableListOf<Flow<Podcast>>()
                        val chunks = feeds.chunked(5)

                        // Process each chunk
                        chunks.forEachIndexed { index, chunk ->
                            // Create flows in parallel for this chunk
                            val chunkFlows = chunk.map { feed ->
                                async {
                                    podcastRepository.getPodcastByFeedId(feed.id)
                                        .filterNotNull()
                                }
                            }.awaitAll()

                            allPodcastFlows.addAll(chunkFlows)

                            val isLastChunk = index == chunks.lastIndex
                            if (isLastChunk) {
                                // Last chunk: collect continuously for real-time updates
                                combine(allPodcastFlows) { podcasts ->
                                    podcasts.toList()
                                }.collect { podcasts ->
                                    send(podcasts)
                                }
                            } else {
                                // Not last chunk: emit once for progressive loading
                                val currentPodcasts = combine(allPodcastFlows) { podcasts ->
                                    podcasts.toList()
                                }.first()
                                send(currentPodcasts)
                            }
                        }
                    }
                }
            }
        }
    }
}