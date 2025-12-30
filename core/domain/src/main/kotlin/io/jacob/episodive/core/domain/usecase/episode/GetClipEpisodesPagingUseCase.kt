package io.jacob.episodive.core.domain.usecase.episode

import androidx.paging.PagingData
import io.jacob.episodive.core.domain.repository.EpisodeRepository
import io.jacob.episodive.core.model.Episode
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetClipEpisodesPagingUseCase @Inject constructor(
    private val episodeRepository: EpisodeRepository,
) {
    operator fun invoke(max: Int): Flow<PagingData<Episode>> {
        return episodeRepository.getSoundbiteEpisodesPaging(max)
    }
}
