package io.jacob.episodive.core.domain.usecase.podcast

import io.jacob.episodive.core.domain.repository.PodcastRepository
import io.jacob.episodive.core.domain.repository.UserRepository
import io.jacob.episodive.core.model.Podcast
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject

class GetUserRecommendedPodcastsUseCase @Inject constructor(
    private val userRepository: UserRepository,
    private val podcastRepository: PodcastRepository,
) {
    operator fun invoke(max: Int): Flow<List<Podcast>> {
        return userRepository.getUserData().flatMapLatest { userData ->
            if (userData.categories.isEmpty()) {
                flowOf(emptyList())
            } else {
                podcastRepository.getRecommendedPodcasts(
                    max = max,
                    language = userData.language,
                    includeCategories = userData.categories
                )
            }
        }
    }
}