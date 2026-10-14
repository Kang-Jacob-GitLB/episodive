package io.jacob.episodive.core.domain.usecase.caption

import io.jacob.episodive.core.common.EpisodivePlayers
import io.jacob.episodive.core.common.Player
import io.jacob.episodive.core.domain.repository.CaptionRepository
import io.jacob.episodive.core.domain.repository.PlayerRepository
import io.jacob.episodive.core.domain.repository.UserRepository
import io.jacob.episodive.core.model.caption.CaptionAvailability
import io.jacob.episodive.core.model.caption.CaptionSubject
import io.jacob.episodive.core.model.caption.LiveCaptionState
import io.jacob.episodive.core.model.caption.toCaptionLanguage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * 라디오 시트가 그릴 자막 상태. 에피소드용 [ObserveLiveCaptionUseCase] 와 같은 모델·세션 루프를 쓴다.
 *
 * 입력은 공유 토글(`isCaptionEnabled`)과 `nowPlayingStation` 의 (id, language) 뿐이다. `progress`·
 * `nowPlaying` 과 combine 하지 않는다 — 스테이션 여부는 `nowPlayingStation` 하나로만 가른다. 같은 id 가
 * 다시 발행돼도(감독자 재시작) 쌍이 같아 세션이 유지된다.
 */
class ObserveStationCaptionUseCase @Inject constructor(
    @param:Player(EpisodivePlayers.Main) private val playerRepository: PlayerRepository,
    private val userRepository: UserRepository,
    private val captionRepository: CaptionRepository,
) {
    operator fun invoke(): Flow<LiveCaptionState> {
        val isEnabled = userRepository.getUserData().map { it.isCaptionEnabled }.distinctUntilChanged()
        val station = playerRepository.nowPlayingStation
            .map { it?.id to it?.language }
            .distinctUntilChanged()

        return combine(isEnabled, station) { enabled, (id, language) -> Triple(enabled, id, language) }
            .distinctUntilChanged()
            .flatMapLatest { (enabled, id, language) -> observe(enabled, id, language) }
    }

    private fun observe(isEnabled: Boolean, stationId: String?, language: String?): Flow<LiveCaptionState> {
        if (stationId == null) {
            return flowOf(LiveCaptionState(isEnabled = isEnabled, availability = CaptionAvailability.Unknown, caption = null))
        }
        // 음악 채널처럼 받아쓸 말이 없는 방송은 language 가 null 이다.
        val captionLanguage = language.toCaptionLanguage()
            ?: return flowOf(LiveCaptionState(isEnabled = isEnabled, availability = CaptionAvailability.Unsupported, caption = null))

        // 꺼져 있어도 가용성은 계산한다 — 토글이 이 값으로 "켜면서 바로 받을지" 를 정한다.
        return if (isEnabled) {
            captionRepository.observeSession(CaptionSubject.Station(stationId), captionLanguage)
        } else {
            captionRepository.observeModelOnly(captionLanguage)
        }
    }
}
