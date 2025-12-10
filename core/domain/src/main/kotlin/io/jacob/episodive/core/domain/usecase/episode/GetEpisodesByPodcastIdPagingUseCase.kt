package io.jacob.episodive.core.domain.usecase.episode

import androidx.paging.PagingData
import io.jacob.episodive.core.domain.repository.EpisodeRepository
import io.jacob.episodive.core.model.Episode
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetEpisodesByPodcastIdPagingUseCase @Inject constructor(
    private val episodeRepository: EpisodeRepository,
) {
    operator fun invoke(podcastId: Long): Flow<PagingData<Episode>> {
        return episodeRepository.getEpisodesByFeedIdPaging(
            feedId = podcastId,
        )
    }
}