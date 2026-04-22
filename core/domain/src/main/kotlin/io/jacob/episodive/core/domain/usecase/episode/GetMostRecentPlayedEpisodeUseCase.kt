package io.jacob.episodive.core.domain.usecase.episode

import io.jacob.episodive.core.domain.repository.EpisodeRepository
import io.jacob.episodive.core.model.Episode
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * 가장 최근에 재생된 단일 Episode 를 반환한다.
 *
 * 용도: process 재시작 직후 ExoPlayer 가 이전 세션을 이어 재생하지만 앱 내
 * `_nowPlaying` StateFlow 는 null 인 상태에서 위젯/UI 가 빈 상태로 보이는 문제를
 * 해결하기 위해, MediaNotificationService.onCreate 에서 1회 호출해 hydration.
 */
class GetMostRecentPlayedEpisodeUseCase @Inject constructor(
    private val episodeRepository: EpisodeRepository,
) {
    suspend operator fun invoke(): Episode? =
        episodeRepository.getPlayedEpisodes(max = 1).first().firstOrNull()
}
