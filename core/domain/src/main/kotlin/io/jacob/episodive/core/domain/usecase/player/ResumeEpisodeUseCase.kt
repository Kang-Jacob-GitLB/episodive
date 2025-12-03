package io.jacob.episodive.core.domain.usecase.player

import io.jacob.episodive.core.common.EpisodivePlayers
import io.jacob.episodive.core.common.Player
import io.jacob.episodive.core.domain.repository.PlayerRepository
import io.jacob.episodive.core.model.Episode
import io.jacob.episodive.core.model.mapper.toLongMillis
import javax.inject.Inject

class ResumeEpisodeUseCase @Inject constructor(
    @param:Player(EpisodivePlayers.Main) private val playerRepository: PlayerRepository,
) {
    operator fun invoke(playedEpisode: Episode) {
        playerRepository.play(playedEpisode)
        playerRepository.seekTo(playedEpisode.position.toLongMillis())
    }
}