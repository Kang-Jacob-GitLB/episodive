package io.jacob.episodive.core.domain.usecase.search

import io.jacob.episodive.core.domain.repository.RecentSearchRepository
import io.jacob.episodive.core.model.RecentSearch
import javax.inject.Inject

class DeleteRecentSearchUseCase @Inject constructor(
    private val recentSearchRepository: RecentSearchRepository
) {
    suspend operator fun invoke(recentSearch: RecentSearch) {
        recentSearchRepository.deleteRecentSearch(recentSearch)
    }
}