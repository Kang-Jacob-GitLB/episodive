package io.jacob.episodive.core.domain.usecase.caption

import io.jacob.episodive.core.common.EpisodivePlayers
import io.jacob.episodive.core.common.Player
import io.jacob.episodive.core.domain.repository.CaptionRepository
import io.jacob.episodive.core.domain.repository.PlayerRepository
import io.jacob.episodive.core.domain.repository.UserRepository
import io.jacob.episodive.core.model.Episode
import io.jacob.episodive.core.model.caption.CaptionAvailability
import io.jacob.episodive.core.model.caption.CaptionLanguage
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
 * 플레이어 화면이 그릴 자막 상태 전체를 계산한다.
 *
 * 에피소드 id 는 반드시 `playerRepository.progress.episodeId` 에서만 가져온다. `nowPlaying` 과
 * combine 해 id 를 얻지 않는다 — `nowPlaying` 은 Room 왕복을 거쳐 `progress` 보다 늦게 도착하므로
 * 전환 순간 어긋난 (이전 progress id, 새 nowPlaying) 쌍이 만들어진다. 그래서 둘이 가리키는
 * 에피소드가 일치할 때만(`Target.Unknown` 아님) 세션을 연다.
 */
class ObserveLiveCaptionUseCase @Inject constructor(
    @param:Player(EpisodivePlayers.Main) private val playerRepository: PlayerRepository,
    private val userRepository: UserRepository,
    private val captionRepository: CaptionRepository,
) {
    operator fun invoke(): Flow<LiveCaptionState> {
        val isEnabled = userRepository.getUserData().map { it.isCaptionEnabled }.distinctUntilChanged()
        val episodeId = playerRepository.progress.map { it.episodeId }.distinctUntilChanged()

        return combine(isEnabled, episodeId, playerRepository.nowPlaying, ::toSelection)
            .distinctUntilChanged()
            .flatMapLatest(::observe)
    }

    /** 판정에 쓴 입력을 한 시점의 값으로 묶어, 이후 계산은 이 결과에만 의존하게 만든다. */
    private sealed interface Target {
        data object Unknown : Target
        data class Transcript(val episodeId: Long, val feedLanguage: String) : Target
        data object Unsupported : Target
        data class Episode(val episodeId: Long, val language: CaptionLanguage) : Target
    }

    /**
     * 켜짐 여부와 대상을 따로 싣는다. 꺼져 있어도 대상의 가용성(모델이 있는가, 미지원인가)은
     * 계산해야 한다 — 토글은 [LiveCaptionState] 하나만 보고 "켜면서 곧바로 모델을 받을지" 를
     * 정하는데, 꺼진 동안 가용성을 `Unknown` 으로 뭉개면 첫 탭은 켜기만 하고 두 번째 탭에서야
     * 다운로드가 시작된다(기기에서 실제로 그랬다).
     */
    private data class Selection(val isEnabled: Boolean, val target: Target)

    private fun toSelection(isEnabled: Boolean, episodeId: Long?, nowPlaying: Episode?): Selection =
        Selection(isEnabled, toTarget(episodeId, nowPlaying))

    private fun toTarget(episodeId: Long?, nowPlaying: Episode?): Target {
        // progress 와 nowPlaying 이 가리키는 에피소드가 다른 동안(전환 중)은 세션을 열지 않는다.
        if (episodeId == null || nowPlaying == null || nowPlaying.id != episodeId) return Target.Unknown
        if (nowPlaying.transcriptUrl != null) return Target.Transcript(episodeId, nowPlaying.feedLanguage)
        val language = nowPlaying.feedLanguage.toCaptionLanguage() ?: return Target.Unsupported
        return Target.Episode(episodeId, language)
    }

    private fun observe(selection: Selection): Flow<LiveCaptionState> {
        val isEnabled = selection.isEnabled
        return when (val target = selection.target) {
            Target.Unknown -> flowOf(LiveCaptionState(isEnabled = isEnabled, availability = CaptionAvailability.Unknown, caption = null))
            Target.Unsupported -> flowOf(LiveCaptionState(isEnabled = isEnabled, availability = CaptionAvailability.Unsupported, caption = null))

            // 꺼져 있으면 번역하지 않는다. VTT 원문 자체는 이 흐름과 무관하게 cue 로 늘 보인다.
            is Target.Transcript -> if (!isEnabled) {
                flowOf(LiveCaptionState(isEnabled = false, availability = CaptionAvailability.Transcript, caption = null))
            } else {
                captionRepository
                    .translatedCues(CaptionSubject.Episode(target.episodeId), target.feedLanguage, playerRepository.cue, playerRepository.seeks)
                    .map { caption -> LiveCaptionState(isEnabled = true, availability = CaptionAvailability.Transcript, caption = caption) }
            }

            is Target.Episode -> if (!isEnabled) {
                captionRepository.observeModelOnly(target.language)
            } else {
                captionRepository.observeSession(CaptionSubject.Episode(target.episodeId), target.language)
            }
        }
    }
}
