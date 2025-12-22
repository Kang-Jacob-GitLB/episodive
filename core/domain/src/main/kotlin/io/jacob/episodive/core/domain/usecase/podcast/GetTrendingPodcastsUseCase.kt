package io.jacob.episodive.core.domain.usecase.podcast

import io.jacob.episodive.core.domain.repository.PodcastRepository
import io.jacob.episodive.core.model.Category
import io.jacob.episodive.core.model.Podcast
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetTrendingPodcastsUseCase @Inject constructor(
    private val podcastRepository: PodcastRepository,
) {
    operator fun invoke(
        max: Int,
        language: String? = null,
        categories: List<Category> = emptyList(),
    ): Flow<List<Podcast>> {
        return podcastRepository.getTrendingPodcasts(max, language, categories)
    }
}