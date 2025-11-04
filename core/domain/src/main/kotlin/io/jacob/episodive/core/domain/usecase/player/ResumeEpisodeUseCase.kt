package io.jacob.episodive.core.domain.usecase.player

import io.jacob.episodive.core.domain.di.MainPlayerRepository
import io.jacob.episodive.core.domain.repository.PlayerRepository
import io.jacob.episodive.core.model.Episode
import io.jacob.episodive.core.model.mapper.toLongMillis
import javax.inject.Inject

class ResumeEpisodeUseCase @Inject constructor(
    @MainPlayerRepository private val playerRepository: PlayerRepository,
) {
    operator fun invoke(playedEpisode: Episode) {
        playerRepository.play(playedEpisode)
        playerRepository.seekTo(playedEpisode.position.toLongMillis())
    }
}