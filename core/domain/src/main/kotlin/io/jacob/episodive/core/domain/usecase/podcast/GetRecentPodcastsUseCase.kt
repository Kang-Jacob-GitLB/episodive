package io.jacob.episodive.core.domain.usecase.podcast

import io.jacob.episodive.core.domain.repository.FeedRepository
import io.jacob.episodive.core.domain.repository.PodcastRepository
import io.jacob.episodive.core.model.Category
import io.jacob.episodive.core.model.Podcast
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class GetRecentPodcastsUseCase @Inject constructor(
    private val feedRepository: FeedRepository,
    private val podcastRepository: PodcastRepository,
) {
    operator fun invoke(
        language: String? = null,
        categories: List<Category> = emptyList()
    ): Flow<List<Podcast>> {
        return feedRepository.getRecentFeeds(
            language = language,
            includeCategories = categories
        ).flatMapLatest { feeds ->
            flow {
                val podcasts = mutableListOf<Podcast>()
                feeds.chunked(5).forEach { chunk ->
                    val chunkPodcasts = coroutineScope {
                        chunk.map { feed ->
                            async {
                                podcastRepository.getPodcastByFeedId(feed.id).first()
                            }
                        }.awaitAll().filterNotNull()
                    }
                    podcasts.addAll(chunkPodcasts)
                    emit(podcasts.toList())
                }
            }
        }
    }
}