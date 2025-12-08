package io.jacob.episodive.core.domain.usecase.episode

import io.jacob.episodive.core.domain.repository.EpisodeRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class IsLikedUseCase @Inject constructor(
    private val episodeRepository: EpisodeRepository,
) {
    operator fun invoke(id: Long): Flow<Boolean> {
        return episodeRepository.isLiked(id)
    }
}