package io.jacob.episodive.core.domain.usecase.episode

import io.jacob.episodive.core.domain.repository.EpisodeRepository
import io.jacob.episodive.core.model.Progress
import javax.inject.Inject
import kotlin.time.Duration

class UpdatePlayedEpisodeUseCase @Inject constructor(
    private val episodeRepository: EpisodeRepository
) {
    suspend operator fun invoke(episodeId: Long, progress: Progress) {
        episodeRepository.updatePlayed(
            id = episodeId,
            position = progress.position,
            isCompleted = progress.positionRatio >= 1f,
        )
        if (progress.duration > Duration.ZERO) {
            episodeRepository.updateDurationOfEpisodes(
                id = episodeId,
                duration = progress.duration,
            )
        }
    }
}