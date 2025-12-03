package io.jacob.episodive.core.domain.usecase.player

import io.jacob.episodive.core.common.EpisodivePlayers
import io.jacob.episodive.core.common.Player
import io.jacob.episodive.core.domain.repository.PlayerRepository
import io.jacob.episodive.core.model.Episode
import javax.inject.Inject

class PlayEpisodeUseCase @Inject constructor(
    @param:Player(EpisodivePlayers.Main) private val playerRepository: PlayerRepository,
) {
    operator fun invoke(episode: Episode) {
        playerRepository.play(episode)
    }
}