package io.jacob.episodive.core.domain.usecase.episode

import androidx.paging.PagingData
import androidx.paging.flatMap
import io.jacob.episodive.core.domain.repository.EpisodeRepository
import io.jacob.episodive.core.domain.repository.FeedRepository
import io.jacob.episodive.core.model.Episode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class GetClipEpisodesPagingUseCase @Inject constructor(
    private val feedRepository: FeedRepository,
    private val episodeRepository: EpisodeRepository,
) {
    operator fun invoke(): Flow<PagingData<Episode>> {
        return feedRepository.getRecentSoundbitesPaging().map { pagingData ->
            pagingData.flatMap { soundbite ->
                val episode = episodeRepository.getEpisodeById(soundbite.episodeId).first()
                episode?.let { listOf(it) } ?: emptyList()
            }
        }
    }
}
