package io.jacob.episodive.core.domain.usecase

import io.jacob.episodive.core.domain.repository.EpisodeRepository
import io.jacob.episodive.core.domain.repository.PodcastRepository
import io.jacob.episodive.core.model.SearchResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject

class SearchUseCase @Inject constructor(
    private val podcastRepository: PodcastRepository,
    private val episodeRepository: EpisodeRepository,
) {
    operator fun invoke(query: String): Flow<SearchResult> {
        return podcastRepository.searchPodcasts(query)
            .flatMapLatest { podcasts ->
                if (podcasts.isEmpty()) {
                    flowOf(SearchResult())
                } else {
                    combine(
                        podcasts.map { podcast ->
                            episodeRepository.getEpisodesByFeedId(
                                feedId = podcast.id,
                                max = 5,
                            )
                        }
                    ) { episodeArrays ->
                        val episodes = episodeArrays.flatMap { it }
                        SearchResult(
                            podcasts = podcasts,
                            episodes = episodes
                        )
                    }
                }
            }
    }
}