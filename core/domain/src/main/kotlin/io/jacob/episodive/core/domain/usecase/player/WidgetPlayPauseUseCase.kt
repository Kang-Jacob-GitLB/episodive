package io.jacob.episodive.core.domain.usecase.player

import io.jacob.episodive.core.common.EpisodivePlayers
import io.jacob.episodive.core.common.Player
import io.jacob.episodive.core.domain.repository.PlayerRepository
import javax.inject.Inject

/**
 * 위젯의 재생/일시정지 버튼.
 *
 * 스테이션을 정지하면 플레이어가 비는데 위젯은 마지막 에피소드를 일시정지로 보여 준다. 빈 플레이어에
 * `play()` 만 부르면 아무 일도 없으므로, 비어 있으면 마지막 재생 상태를 먼저 올린다.
 *
 * **메인에서 호출할 것.** `hasMediaItem()` 이 플레이어를 직접 읽는다.
 */
class WidgetPlayPauseUseCase @Inject constructor(
    @param:Player(EpisodivePlayers.Main) private val playerRepository: PlayerRepository,
    private val restoreLastPlayState: RestoreLastPlayStateUseCase,
) {
    /** 재생 상태를 바꿨으면 true. 올릴 것이 없어 아무것도 못 했으면 false. */
    suspend operator fun invoke(): Boolean {
        if (!playerRepository.hasMediaItem() && !restoreLastPlayState()) return false
        playerRepository.playOrPause()
        return true
    }
}
