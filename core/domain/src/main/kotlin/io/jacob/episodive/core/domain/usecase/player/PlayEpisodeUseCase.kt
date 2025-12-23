package io.jacob.episodive.core.domain.usecase.player

import io.jacob.episodive.core.common.EpisodivePlayers
import io.jacob.episodive.core.common.Player
import io.jacob.episodive.core.domain.repository.EpisodeRepository
import io.jacob.episodive.core.domain.repository.PlayerRepository
import io.jacob.episodive.core.model.Episode
import io.jacob.episodive.core.model.GroupKey
import javax.inject.Inject

class PlayEpisodeUseCase @Inject constructor(
    @param:Player(EpisodivePlayers.Main) private val playerRepository: PlayerRepository,
    private val episodeRepository: EpisodeRepository,
) {
    suspend operator fun invoke(episode: Episode) {
        playerRepository.play(episode)
        episodeRepository.replaceEpisodes(
            episodes = listOf(episode),
            groupKey = GroupKey.PLAYLIST.toString(),
        )
    }

    suspend operator fun invoke(playEpisode: Episode? = null, episodes: List<Episode>) {
        val index = playEpisode?.let {
            episodes.indexOfFirst { it.id == playEpisode.id }.takeIf { it >= 0 }
        } ?: 0
        playerRepository.play(
            episodes = episodes,
            indexToPlay = index,
        )
        episodeRepository.replaceEpisodes(
            episodes = episodes,
            groupKey = GroupKey.PLAYLIST.toString(),
        )
    }
}