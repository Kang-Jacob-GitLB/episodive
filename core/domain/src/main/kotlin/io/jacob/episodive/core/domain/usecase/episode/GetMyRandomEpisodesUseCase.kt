package io.jacob.episodive.core.domain.usecase.episode

import io.jacob.episodive.core.domain.repository.EpisodeRepository
import io.jacob.episodive.core.domain.repository.UserRepository
import io.jacob.episodive.core.model.Episode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import javax.inject.Inject

class GetMyRandomEpisodesUseCase @Inject constructor(
    private val episodeRepository: EpisodeRepository,
    private val userRepository: UserRepository,
) {
    operator fun invoke(max: Int = 6): Flow<List<Episode>> {
        return userRepository.getUserData().flatMapLatest { userData ->
            episodeRepository.getRandomEpisodes(
                max = max,
                language = userData.language,
                includeCategories = userData.categories,
            )
        }
    }
}