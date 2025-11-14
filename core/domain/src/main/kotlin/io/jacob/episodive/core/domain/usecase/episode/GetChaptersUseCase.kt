package io.jacob.episodive.core.domain.usecase.episode

import io.jacob.episodive.core.domain.repository.EpisodeRepository
import io.jacob.episodive.core.model.Chapter
import javax.inject.Inject

class GetChaptersUseCase @Inject constructor(
    private val episodeRepository: EpisodeRepository,
) {
    suspend operator fun invoke(url: String): List<Chapter> {
        return episodeRepository.fetchChapters(url)
    }
}