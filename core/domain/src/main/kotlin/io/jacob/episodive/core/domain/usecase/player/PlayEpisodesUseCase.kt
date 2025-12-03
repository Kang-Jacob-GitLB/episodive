package io.jacob.episodive.core.domain.usecase.player

import io.jacob.episodive.core.common.EpisodivePlayers
import io.jacob.episodive.core.common.Player
import io.jacob.episodive.core.domain.repository.PlayerRepository
import io.jacob.episodive.core.model.Episode
import javax.inject.Inject

class PlayEpisodesUseCase @Inject constructor(
    @param:Player(EpisodivePlayers.Main) private val playerRepository: PlayerRepository,
) {
    operator fun invoke(episodes: List<Episode>, playEpisode: Episode? = null) {
        val index = playEpisode?.let {
            episodes.indexOfFirst { it.id == playEpisode.id }.takeIf { it >= 0 }
        } ?: 0
        playerRepository.play(
            episodes = episodes,
            indexToPlay = index,
        )
    }
}