package io.jacob.episodive.core.domain.usecase.podcast

import io.jacob.episodive.core.domain.repository.FeedRepository
import io.jacob.episodive.core.domain.repository.UserRepository
import io.jacob.episodive.core.model.Podcast
import io.jacob.episodive.core.model.mapper.toFeedsFromRecent
import io.jacob.episodive.core.model.mapper.toFeedsFromTrending
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject

class GetRecommendedPodcastsUseCase @Inject constructor(
    private val userRepository: UserRepository,
    private val feedRepository: FeedRepository,
    private val getPodcastsByFeedIdsParallellyUseCase: GetPodcastsByFeedIdsParallellyUseCase,
) {
    operator fun invoke(): Flow<List<Podcast>> {
        return userRepository.getUserData().flatMapLatest { userData ->
            if (userData.categories.isEmpty()) {
                flowOf(emptyList())
            } else {
                combine(
                    feedRepository.getTrendingFeeds(
                        max = 100,
                        language = userData.language,
                        includeCategories = userData.categories
                    ),
                    feedRepository.getRecentFeeds(
                        max = 100,
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

                    flow {
                        emit(getPodcastsByFeedIdsParallellyUseCase(feeds.map { it.id }))
                    }
                }
            }
        }
    }
}