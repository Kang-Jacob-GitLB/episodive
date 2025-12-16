package io.jacob.episodive.core.domain.usecase.podcast

import androidx.paging.PagingData
import io.jacob.episodive.core.domain.repository.PodcastRepository
import io.jacob.episodive.core.model.Podcast
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetFollowedPodcastsPagingUseCase @Inject constructor(
    private val podcastRepository: PodcastRepository,
) {
    operator fun invoke(query: String? = null): Flow<PagingData<Podcast>> {
        return podcastRepository.getFollowedPodcastsPaging(query)
    }
}