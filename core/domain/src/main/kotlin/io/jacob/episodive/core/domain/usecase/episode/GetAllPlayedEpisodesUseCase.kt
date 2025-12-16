package io.jacob.episodive.core.domain.usecase.episode

import io.jacob.episodive.core.domain.repository.EpisodeRepository
import io.jacob.episodive.core.model.Episode
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetAllPlayedEpisodesUseCase @Inject constructor(
    private val episodeRepository: EpisodeRepository,
) {
    operator fun invoke(query: String? = null, max: Int): Flow<List<Episode>> {
        return episodeRepository.getAllPlayedEpisodes(query, max)
    }
}