package io.jacob.episodive.core.domain.usecase.search

import io.jacob.episodive.core.domain.repository.EpisodeRepository
import io.jacob.episodive.core.domain.repository.PodcastRepository
import io.jacob.episodive.core.model.Episode
import io.jacob.episodive.core.model.SearchResult
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

class SearchUseCase @Inject constructor(
    private val podcastRepository: PodcastRepository,
    private val episodeRepository: EpisodeRepository,
) {
    operator fun invoke(query: String, max: Int): Flow<SearchResult> {
        return channelFlow {
            val podcasts = podcastRepository.searchPodcasts(query, max).first()

            if (podcasts.isEmpty()) {
                send(SearchResult())
            } else {
                Timber.w("podcast result size: ${podcasts.size}")
                val episodeMap = mutableMapOf<Long, List<Episode>>()

                // 초기 liked episodes를 한 번만 가져옴
                val initialLikedEpisodes = episodeRepository.getLikedEpisodes(max = 10000).first()

                // 에피소드들을 점진적으로 로드
                coroutineScope {
                    podcasts.forEach { podcast ->
                        launch {
                            episodeRepository.getEpisodesByFeedId(
                                feedId = podcast.id,
                                max = 5,
                            ).first().let { episodes ->
                                synchronized(episodeMap) {
                                    episodeMap[podcast.id] = episodes.take(5)
                                }

                                // 로드될 때마다 초기 liked와 결합하여 즉시 emit
                                val allEpisodes = podcasts.mapNotNull {
                                    episodeMap[it.id]
                                }.flatten().map { episode ->
                                    episode.copy(likedAt = initialLikedEpisodes.find { liked ->
                                        liked.id == episode.id
                                    }?.likedAt)
                                }
                                Timber.w("episode result size: ${allEpisodes.size}")

                                send(
                                    SearchResult(
                                        podcasts = podcasts,
                                        episodes = allEpisodes
                                    )
                                )
                            }
                        }
                    }
                }

                // 모든 에피소드 로드 완료 후, liked episodes 변화를 계속 관찰
                episodeRepository.getLikedEpisodes(max = 10000).collect { likedEpisodes ->
                    val allEpisodes = synchronized(episodeMap) {
                        podcasts.mapNotNull { episodeMap[it.id] }.flatten()
                    }.map { episode ->
                        episode.copy(likedAt = likedEpisodes.find { liked ->
                            liked.id == episode.id
                        }?.likedAt)
                    }

                    send(
                        SearchResult(
                            podcasts = podcasts,
                            episodes = allEpisodes
                        )
                    )
                }
            }
        }
    }
}