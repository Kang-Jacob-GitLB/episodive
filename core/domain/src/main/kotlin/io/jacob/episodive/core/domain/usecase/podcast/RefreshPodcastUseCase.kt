package io.jacob.episodive.core.domain.usecase.podcast

import io.jacob.episodive.core.domain.repository.EpisodeRepository
import io.jacob.episodive.core.domain.repository.PodcastRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import javax.inject.Inject

/**
 * 팟캐스트 화면을 원격에서 다시 받는다. 정보(헤더·에피소드 수)와 에피소드 목록을 함께 받는다 — 목록만 받으면
 * 새 에피소드가 늘었는데 "전체 에피소드 N" 은 낡은 캐시 값으로 남아 둘이 어긋난다.
 * 하나라도 실패하면 그 실패를 던진다.
 */
class RefreshPodcastUseCase @Inject constructor(
    private val podcastRepository: PodcastRepository,
    private val episodeRepository: EpisodeRepository,
) {
    suspend operator fun invoke(podcastId: Long) {
        coroutineScope {
            awaitAll(
                async { podcastRepository.refreshPodcastByFeedId(podcastId) },
                async { episodeRepository.refreshEpisodesByFeedId(podcastId) },
            )
        }
    }
}
