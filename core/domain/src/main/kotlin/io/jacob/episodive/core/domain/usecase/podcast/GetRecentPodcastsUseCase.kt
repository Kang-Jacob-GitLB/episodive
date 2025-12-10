package io.jacob.episodive.core.domain.usecase.podcast

import io.jacob.episodive.core.domain.repository.FeedRepository
import io.jacob.episodive.core.model.Category
import io.jacob.episodive.core.model.Podcast
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class GetRecentPodcastsUseCase @Inject constructor(
    private val feedRepository: FeedRepository,
    private val getPodcastsByFeedIdsUseCase: GetPodcastsByFeedIdsUseCase,
) {
    operator fun invoke(
        max: Int = 10,
        language: String? = null,
        categories: List<Category> = emptyList(),
    ): Flow<List<Podcast>> {
        return feedRepository.getRecentFeeds(
            max = max,
            language = language,
            includeCategories = categories
        ).map { feeds ->
            getPodcastsByFeedIdsUseCase(feeds.map { it.id })
        }
    }
}