package io.jacob.episodive.core.domain.usecase.episode

import io.jacob.episodive.core.domain.repository.EpisodeRepository
import io.jacob.episodive.core.model.Episode
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetEpisodesByPodcastIdUseCase @Inject constructor(
    private val episodeRepository: EpisodeRepository
) {
    operator fun invoke(podcastId: Long): Flow<List<Episode>> {
        return episodeRepository.getEpisodesByFeedId(
            feedId = podcastId,
            max = 10000,
        )
    }
}