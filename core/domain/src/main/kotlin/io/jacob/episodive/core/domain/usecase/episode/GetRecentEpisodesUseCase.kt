package io.jacob.episodive.core.domain.usecase.episode

import io.jacob.episodive.core.domain.repository.EpisodeRepository
import io.jacob.episodive.core.model.Episode
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetRecentEpisodesUseCase @Inject constructor(
    private val episodeRepository: EpisodeRepository,
) {
    operator fun invoke(): Flow<List<Episode>> {
        return episodeRepository.getRecentEpisodes(max = 6)
    }
}