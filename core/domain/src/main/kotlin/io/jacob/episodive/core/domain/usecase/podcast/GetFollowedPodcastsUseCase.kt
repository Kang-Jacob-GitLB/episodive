package io.jacob.episodive.core.domain.usecase.podcast

import io.jacob.episodive.core.domain.repository.PodcastRepository
import io.jacob.episodive.core.model.Podcast
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetFollowedPodcastsUseCase @Inject constructor(
    private val podcastRepository: PodcastRepository,
) {
    operator fun invoke(query: String? = null, max: Int): Flow<List<Podcast>> {
        return podcastRepository.getFollowedPodcasts(query, max)
    }
}