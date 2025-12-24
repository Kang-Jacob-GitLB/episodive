package io.jacob.episodive.core.domain.usecase.player

import io.jacob.episodive.core.common.EpisodivePlayers
import io.jacob.episodive.core.common.Player
import io.jacob.episodive.core.domain.repository.EpisodeRepository
import io.jacob.episodive.core.domain.repository.PlayerRepository
import io.jacob.episodive.core.model.Episode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject

class GetNowPlayingUseCase @Inject constructor(
    @param:Player(EpisodivePlayers.Main) private val playerRepository: PlayerRepository,
    private val episodeRepository: EpisodeRepository,
) {
    operator fun invoke(): Flow<Episode?> {
        return playerRepository.nowPlaying.flatMapLatest { episode ->
            episode?.let { episodeRepository.getEpisodeById(it.id) } ?: flowOf(null)
        }
    }
}