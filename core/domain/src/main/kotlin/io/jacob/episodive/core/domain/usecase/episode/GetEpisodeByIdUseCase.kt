package io.jacob.episodive.core.domain.usecase.episode

import io.jacob.episodive.core.domain.repository.EpisodeRepository
import io.jacob.episodive.core.model.Episode
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetEpisodeByIdUseCase @Inject constructor(
    private val episodeRepository: EpisodeRepository,
) {
    operator fun invoke(id: Long): Flow<Episode?> {
        return episodeRepository.getEpisodeById(id)
    }
}
