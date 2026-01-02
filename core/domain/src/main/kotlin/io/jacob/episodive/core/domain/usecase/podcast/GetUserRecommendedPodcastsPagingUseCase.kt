package io.jacob.episodive.core.domain.usecase.podcast

import androidx.paging.PagingData
import io.jacob.episodive.core.domain.repository.PodcastRepository
import io.jacob.episodive.core.domain.repository.UserRepository
import io.jacob.episodive.core.model.Podcast
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject

class GetUserRecommendedPodcastsPagingUseCase @Inject constructor(
    private val userRepository: UserRepository,
    private val podcastRepository: PodcastRepository,
) {
    operator fun invoke(max: Int): Flow<PagingData<Podcast>> {
        return userRepository.getUserData().flatMapLatest { userData ->
            if (userData.categories.isEmpty()) {
                flowOf(PagingData.empty())
            } else {
                podcastRepository.getRecommendedPodcastsPaging(
                    max = max,
                    language = userData.language,
                    includeCategories = userData.categories
                )
            }
        }
    }
}