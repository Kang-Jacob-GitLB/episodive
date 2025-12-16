package io.jacob.episodive.core.domain.usecase.podcast

import io.jacob.episodive.core.domain.repository.FeedRepository
import io.jacob.episodive.core.model.Category
import io.jacob.episodive.core.model.Podcast
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class GetTrendingPodcastsUseCase @Inject constructor(
    private val feedRepository: FeedRepository,
    private val getPodcastsByFeedIdsParallellyUseCase: GetPodcastsByFeedIdsParallellyUseCase,
) {
    operator fun invoke(
        max: Int,
        language: String? = null,
        categories: List<Category> = emptyList(),
    ): Flow<List<Podcast>> {
        return feedRepository.getTrendingFeeds(
            max = max,
            language = language,
            includeCategories = categories,
        ).map { feeds ->
            getPodcastsByFeedIdsParallellyUseCase(feeds.map { it.id })
        }
    }
}