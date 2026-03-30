package io.jacob.episodive.core.data.repository

import io.jacob.episodive.core.database.datasource.RecentSearchLocalDataSource
import io.jacob.episodive.core.database.model.RecentSearchEntity
import io.jacob.episodive.core.domain.repository.RecentSearchRepository
import io.jacob.episodive.core.model.Episode
import io.jacob.episodive.core.model.Podcast
import io.jacob.episodive.core.model.RecentSearch
import io.jacob.episodive.core.model.RecentSearchType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import kotlin.time.Clock

class RecentSearchRepositoryImpl @Inject constructor(
    private val recentSearchLocalDataSource: RecentSearchLocalDataSource,
) : RecentSearchRepository {
    override fun getRecentSearches(limit: Int): Flow<List<RecentSearch>> {
        return recentSearchLocalDataSource.getRecentSearches(limit).map { entities ->
            entities.mapNotNull { it.toRecentSearch() }
        }
    }

    override suspend fun upsertRecentSearch(query: String) {
        recentSearchLocalDataSource.upsertRecentSearch(
            RecentSearchEntity(
                type = RecentSearchType.QUERY,
                query = query,
                searchedAt = Clock.System.now(),
            )
        )
    }

    override suspend fun upsertRecentSearch(podcast: Podcast) {
        recentSearchLocalDataSource.upsertRecentSearch(
            RecentSearchEntity(
                type = RecentSearchType.PODCAST,
                contentId = podcast.id,
                title = podcast.title,
                imageUrl = podcast.artwork.ifEmpty { podcast.image },
                subtitle = podcast.ownerName.ifEmpty { podcast.author },
                searchedAt = Clock.System.now(),
            )
        )
    }

    override suspend fun upsertRecentSearch(episode: Episode) {
        recentSearchLocalDataSource.upsertRecentSearch(
            RecentSearchEntity(
                type = RecentSearchType.EPISODE,
                contentId = episode.id,
                title = episode.title,
                imageUrl = episode.image.ifEmpty { episode.feedImage },
                subtitle = episode.feedTitle ?: episode.feedAuthor ?: "",
                searchedAt = Clock.System.now(),
            )
        )
    }

    override suspend fun deleteRecentSearch(recentSearch: RecentSearch) {
        recentSearchLocalDataSource.deleteRecentSearch(recentSearch.id)
    }

    override suspend fun clearRecentSearches() {
        recentSearchLocalDataSource.clearRecentSearches()
    }

    private fun RecentSearchEntity.toRecentSearch(): RecentSearch? {
        return when (type) {
            RecentSearchType.QUERY -> RecentSearch.Query(
                id = id,
                query = query ?: return null,
                searchedAt = searchedAt,
            )
            RecentSearchType.PODCAST -> RecentSearch.PodcastSearch(
                id = id,
                podcastId = contentId ?: return null,
                title = title ?: return null,
                imageUrl = imageUrl ?: "",
                author = subtitle ?: "",
                searchedAt = searchedAt,
            )
            RecentSearchType.EPISODE -> RecentSearch.EpisodeSearch(
                id = id,
                episodeId = contentId ?: return null,
                title = title ?: return null,
                imageUrl = imageUrl ?: "",
                feedTitle = subtitle ?: "",
                searchedAt = searchedAt,
            )
        }
    }
}
