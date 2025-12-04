package io.jacob.episodive.core.domain.usecase.player

import io.jacob.episodive.core.common.EpisodivePlayers
import io.jacob.episodive.core.common.Player
import io.jacob.episodive.core.domain.repository.PlayerRepository
import io.jacob.episodive.core.model.Episode
import kotlinx.coroutines.flow.first
import timber.log.Timber
import javax.inject.Inject

class PlayAndAddClipsUseCase @Inject constructor(
    @param:Player(EpisodivePlayers.Clip) private val playerRepository: PlayerRepository,
) {
    private var isPlayedOnce = false

    suspend operator fun invoke(episodes: List<Episode>) {
        Timber.i("PlayWhenReady size: ${episodes.size}")
        if (episodes.isEmpty()) return

        if (!isPlayedOnce) {
            playerRepository.playClips(
                episodes = episodes,
            )
            isPlayedOnce = true
        } else {
            val currentPlaylist = playerRepository.playlist.first()
            val currentEpisodeIds = currentPlaylist.map { it.id }.toSet()

            val newEpisodes = episodes.filter { episode ->
                episode.id !in currentEpisodeIds
            }

            if (newEpisodes.isNotEmpty()) {
                Timber.i("Adding ${newEpisodes.size} new clips to playlist")
                playerRepository.addClipTracks(
                    episodes = newEpisodes
                )
            } else {
                Timber.i("No new clips to add")
            }
        }
    }
}