package io.jacob.episodive.core.domain.usecase.search

import io.jacob.episodive.core.domain.repository.RecentSearchRepository
import io.jacob.episodive.core.model.RecentSearch
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetRecentSearchesUseCase @Inject constructor(
    private val recentSearchRepository: RecentSearchRepository
) {
    operator fun invoke(limit: Int): Flow<List<RecentSearch>> {
        return recentSearchRepository.getRecentSearches(limit)
    }
}