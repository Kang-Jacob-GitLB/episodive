package io.jacob.episodive.core.domain.usecase.podcast

import io.jacob.episodive.core.domain.repository.PodcastRepository
import javax.inject.Inject

class ToggleFollowedUseCase @Inject constructor(
    private val podcastRepository: PodcastRepository,
) {
    suspend operator fun invoke(id: Long): Boolean {
        return podcastRepository.toggleFollowed(id)
    }
}