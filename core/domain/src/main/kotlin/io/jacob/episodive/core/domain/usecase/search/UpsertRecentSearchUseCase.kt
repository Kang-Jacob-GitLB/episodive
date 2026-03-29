package io.jacob.episodive.core.domain.usecase.search

import io.jacob.episodive.core.domain.repository.RecentSearchRepository
import io.jacob.episodive.core.model.Episode
import io.jacob.episodive.core.model.Podcast
import javax.inject.Inject

class UpsertRecentSearchUseCase @Inject constructor(
    private val recentSearchRepository: RecentSearchRepository
) {
    suspend operator fun invoke(query: String) {
        recentSearchRepository.upsertRecentSearch(query)
    }

    suspend operator fun invoke(podcast: Podcast) {
        recentSearchRepository.upsertRecentSearch(podcast)
    }

    suspend operator fun invoke(episode: Episode) {
        recentSearchRepository.upsertRecentSearch(episode)
    }
}