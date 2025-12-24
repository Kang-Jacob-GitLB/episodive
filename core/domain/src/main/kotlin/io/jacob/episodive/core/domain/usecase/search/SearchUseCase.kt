package io.jacob.episodive.core.domain.usecase.search

import io.jacob.episodive.core.domain.repository.EpisodeRepository
import io.jacob.episodive.core.domain.repository.PodcastRepository
import io.jacob.episodive.core.model.Episode
import io.jacob.episodive.core.model.SearchResult
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

class SearchUseCase @Inject constructor(
    private val podcastRepository: PodcastRepository,
    private val episodeRepository: EpisodeRepository,
) {
    operator fun invoke(query: String, max: Int): Flow<SearchResult> {
        return podcastRepository.searchPodcasts(query, max)
            .flatMapLatest { podcasts ->
                if (podcasts.isEmpty()) {
                    flowOf(SearchResult())
                } else {
                    Timber.w("podcast result size: ${podcasts.size}")

                    channelFlow {
                        val episodeMap = mutableMapOf<Long, List<Episode>>()

                        coroutineScope {
                            podcasts.forEach { podcast ->
                                launch {
                                    episodeRepository.getEpisodesByFeedId(
                                        feedId = podcast.id,
                                        max = 5,
                                    ).collectLatest { episodes ->
                                        synchronized(episodeMap) {
                                            episodeMap[podcast.id] = episodes.take(5)
                                        }
                                        val allEpisodes = podcasts.mapNotNull {
                                            episodeMap[it.id]
                                        }.flatten()

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
                }
            }
    }
}