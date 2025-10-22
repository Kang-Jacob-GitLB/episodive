package io.jacob.episodive.core.domain.usecase.player

import io.jacob.episodive.core.domain.di.MainPlayerRepository
import io.jacob.episodive.core.domain.repository.PlayerRepository
import io.jacob.episodive.core.model.Episode
import javax.inject.Inject

class PlayEpisodeUseCase @Inject constructor(
    @MainPlayerRepository private val playerRepository: PlayerRepository,
) {
    operator fun invoke(episode: Episode) {
        playerRepository.play(episode)
    }
}