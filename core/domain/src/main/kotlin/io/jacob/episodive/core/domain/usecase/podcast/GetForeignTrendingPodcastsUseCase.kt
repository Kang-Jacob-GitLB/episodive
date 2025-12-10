package io.jacob.episodive.core.domain.usecase.podcast

import io.jacob.episodive.core.domain.repository.UserRepository
import io.jacob.episodive.core.model.Podcast
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject

class GetForeignTrendingPodcastsUseCase @Inject constructor(
    private val userRepository: UserRepository,
    private val getTrendingPodcastsUseCase: GetTrendingPodcastsUseCase,
) {
    operator fun invoke(max: Int): Flow<List<Podcast>> {
        return userRepository.getUserData().flatMapLatest { userData ->
            if (userData.categories.isEmpty()) {
                flowOf(emptyList())
            } else {
                val foreignLanguages =
                    languages.filter { it != userData.language }.joinToString(",")
                getTrendingPodcastsUseCase(
                    max = max,
                    language = foreignLanguages,
                )
            }
        }
    }

    companion object {
        private val languages = listOf("en", "es", "fr", "de", "it", "ja", "ko", "pt", "ru", "zh")
    }
}