package io.jacob.episodive.core.domain.usecase.podcast

import io.jacob.episodive.core.domain.repository.PodcastRepository
import io.jacob.episodive.core.model.Channel
import io.jacob.episodive.core.model.Podcast
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetPodcastsByChannelUseCase @Inject constructor(
    private val podcastRepository: PodcastRepository,
) {
    operator fun invoke(channel: Channel): Flow<List<Podcast>> {
        return podcastRepository.getPodcastsByChannel(channel)
    }
}