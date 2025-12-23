package io.jacob.episodive.core.domain.usecase.podcast

import io.jacob.episodive.core.model.Podcast
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

class GetRecommendedPodcastsUseCase @Inject constructor(
    private val getMyTrendingPodcastsUseCase: GetMyTrendingPodcastsUseCase,
    private val getMyRecentPodcastsUseCase: GetMyRecentPodcastsUseCase,
) {
    operator fun invoke(): Flow<List<Podcast>> {
        return combine(
            getMyTrendingPodcastsUseCase(100),
            getMyRecentPodcastsUseCase(100),
        ) { trending, recent ->
            (trending + recent)
                .distinctBy { it.id }
                .sortedByDescending { it.newestItemPublishTime }
        }
    }
}