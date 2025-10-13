package io.jacob.episodive.core.domain.usecase.search

import io.jacob.episodive.core.domain.repository.RecentSearchRepository
import javax.inject.Inject

class ClearRecentSearchesUseCase @Inject constructor(
    private val recentSearchRepository: RecentSearchRepository
) {
    suspend operator fun invoke() {
        recentSearchRepository.clearRecentSearches()
    }
}