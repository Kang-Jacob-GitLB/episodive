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
        val target = playlist[index]
        val dbPositionMs = target.position.inWholeMilliseconds
        // 복원 위치는 항상 "그 에피소드 자신의" 위치여야 한다.
        //
        // 찾은 경우: DataStore 스냅샷과 DB 중 앞선 쪽을 쓴다. 스냅샷은 5초 간격으로만 저장되므로
        // 0.5초마다 갱신되는 DB 보다 최대 그만큼 뒤처져 있고, 그대로 쓰면 앱을 켤 때마다 되감긴다.
        //
        // 못 찾아 인덱스로 폴백한 경우: 스냅샷 위치는 사라진 에피소드의 것이므로 버리고
        // 폴백 대상 자신의 위치를 쓴다. 남의 위치를 물려주면 그 값이 곧바로 저장되어
        // 폴백 대상의 이어듣기 지점이 오염된다.
        val positionMs = if (matchedIndex >= 0) {
            maxOf(lastState.positionMs, dbPositionMs)
        } else {
            dbPositionMs
        }
        // 위의 DB 읽기(suspend)를 기다리는 사이 사용자가 스테이션 등을 올렸을 수 있다. 호출자의 확인은
        // 그 전의 것이므로 prepare 직전에 다시 본다. 둘 다 메인이라 이 확인과 prepare 사이엔 끼어들 수 없다.
        if (playerRepository.hasMediaItem()) return false
        playerRepository.prepare(playlist, index, positionMs)
        playerRepository.setShuffle(lastState.shuffle)
        playerRepository.setRepeat(lastState.repeat)
        return true
    }
}
