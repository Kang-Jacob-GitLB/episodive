package io.jacob.episodive.core.domain.usecase.player

import io.jacob.episodive.core.common.EpisodivePlayers
import io.jacob.episodive.core.common.Player
import io.jacob.episodive.core.domain.repository.EpisodeRepository
import io.jacob.episodive.core.domain.repository.PlayerRepository
import kotlinx.coroutines.flow.flatMapLatest
import javax.inject.Inject

class ObservePlaylistUseCase @Inject constructor(
    @param:Player(EpisodivePlayers.Main) private val playerRepository: PlayerRepository,
    private val episodeRepository: EpisodeRepository,
) {
    val playlist = playerRepository.playlist.flatMapLatest { episodes ->
        episodeRepository.getEpisodesByIds(episodes.map { it.id })
    }
}