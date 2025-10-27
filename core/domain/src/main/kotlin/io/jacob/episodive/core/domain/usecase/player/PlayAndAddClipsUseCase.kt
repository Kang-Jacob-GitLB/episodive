package io.jacob.episodive.core.domain.usecase.player

import io.jacob.episodive.core.domain.di.ClipPlayerRepository
import io.jacob.episodive.core.domain.repository.PlayerRepository
import io.jacob.episodive.core.model.ClipEpisode
import kotlinx.coroutines.flow.first
import timber.log.Timber
import javax.inject.Inject

class PlayAndAddClipsUseCase @Inject constructor(
    @ClipPlayerRepository private val playerRepository: PlayerRepository,
) {
    private var isPlayedOnce = false

    suspend operator fun invoke(clipEpisodes: List<ClipEpisode>) {
        Timber.i("PlayWhenReady size: ${clipEpisodes.size}")
        if (clipEpisodes.isEmpty()) return

        if (!isPlayedOnce) {
            playerRepository.playClips(
                clipEpisodes = clipEpisodes,
            )
            isPlayedOnce = true
        } else {
            val currentPlaylist = playerRepository.playlist.first()
            val currentEpisodeIds = currentPlaylist.map { it.id }.toSet()

            val newClipEpisodes = clipEpisodes.filter { clipEpisode ->
                clipEpisode.episode.id !in currentEpisodeIds
            }

            if (newClipEpisodes.isNotEmpty()) {
                Timber.i("Adding ${newClipEpisodes.size} new clips to playlist")
                playerRepository.addClipTracks(
                    clipEpisodes = newClipEpisodes
                )
            } else {
                Timber.i("No new clips to add")
            }
        }
    }
}