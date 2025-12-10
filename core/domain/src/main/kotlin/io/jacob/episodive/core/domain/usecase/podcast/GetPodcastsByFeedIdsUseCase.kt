package io.jacob.episodive.core.domain.usecase.podcast

import io.jacob.episodive.core.domain.repository.PodcastRepository
import io.jacob.episodive.core.model.Podcast
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class GetPodcastsByFeedIdsUseCase @Inject constructor(
    private val podcastRepository: PodcastRepository,
) {
    suspend operator fun invoke(feedIds: List<Long>): List<Podcast> {
        return coroutineScope {
            feedIds.chunked(5).flatMap { chunk ->
                chunk.map { feedId ->
                    async {
                        podcastRepository.getPodcastByFeedId(feedId).first()
                    }
                }.awaitAll().filterNotNull()
            }
        }
    }
}