package io.jacob.episodive.core.domain.usecase.player

import io.jacob.episodive.core.common.EpisodivePlayers
import io.jacob.episodive.core.common.Player
import io.jacob.episodive.core.domain.repository.EpisodeRepository
import io.jacob.episodive.core.domain.repository.PlayerRepository
import io.jacob.episodive.core.domain.repository.UserRepository
import io.jacob.episodive.core.model.GroupKey
import javax.inject.Inject

class RestoreLastPlayStateUseCase @Inject constructor(
    @param:Player(EpisodivePlayers.Main) private val playerRepository: PlayerRepository,
    private val episodeRepository: EpisodeRepository,
    private val userRepository: UserRepository,
) {
    suspend operator fun invoke(): Boolean {
        val lastState = userRepository.getLastPlayState() ?: return false
        val playlist = episodeRepository.getEpisodesByGroupKey(GroupKey.PLAYLIST.toString())
        if (playlist.isEmpty()) return false

        val matchedIndex = playlist.indexOfFirst { it.id == lastState.episodeId }
        val index = if (matchedIndex >= 0) matchedIndex else lastState.index.coerceIn(0, playlist.size - 1)
        playerRepository.prepare(playlist, index, lastState.positionMs)
        playerRepository.setShuffle(lastState.shuffle)
        playerRepository.setRepeat(lastState.repeat)
        return true
    }
}
