package io.jacob.episodive.core.domain.usecase.episode

import io.jacob.episodive.core.domain.repository.EpisodeRepository
import io.jacob.episodive.core.model.Episode
import javax.inject.Inject

class ToggleLikedEpisodeUseCase @Inject constructor(
    private val episodeRepository: EpisodeRepository,
) {
    suspend operator fun invoke(episode: Episode): Boolean {
        episodeRepository.upsertEpisode(episode)
        return episodeRepository.toggleLikedEpisode(episode)
    }
}