package io.jacob.episodive.core.domain.usecase.podcast

import io.jacob.episodive.core.domain.repository.PodcastRepository
import javax.inject.Inject

class GetChannelsUseCase @Inject constructor(
    private val podcastRepository: PodcastRepository,
) {
    operator fun invoke() = podcastRepository.getChannels()
}