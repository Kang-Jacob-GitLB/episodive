package io.jacob.episodive.core.domain.usecase.episode

import io.jacob.episodive.core.domain.repository.EpisodeRepository
import io.jacob.episodive.core.model.PlayedEpisode
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetAllPlayedEpisodesUseCase @Inject constructor(
    private val episodeRepository: EpisodeRepository,
) {
    operator fun invoke(): Flow<List<PlayedEpisode>> {
        return episodeRepository.getAllPlayedEpisodes()
    }
}