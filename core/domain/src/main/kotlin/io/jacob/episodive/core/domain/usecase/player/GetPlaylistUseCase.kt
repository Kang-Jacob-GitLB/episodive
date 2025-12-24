package io.jacob.episodive.core.domain.usecase.player

import io.jacob.episodive.core.common.EpisodivePlayers
import io.jacob.episodive.core.common.Player
import io.jacob.episodive.core.domain.repository.EpisodeRepository
import io.jacob.episodive.core.domain.repository.PlayerRepository
import io.jacob.episodive.core.model.Episode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import javax.inject.Inject

class GetPlaylistUseCase @Inject constructor(
    @param:Player(EpisodivePlayers.Main) private val playerRepository: PlayerRepository,
    private val episodeRepository: EpisodeRepository,
) {
    operator fun invoke(): Flow<List<Episode>> {
        return playerRepository.playlist.flatMapLatest { episodes ->
            episodeRepository.getEpisodesByIds(episodes.map { it.id })
        }
    }
}