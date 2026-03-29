package io.jacob.episodive.core.model

import kotlin.time.Instant

sealed interface RecentSearch {
    val id: Long
    val searchedAt: Instant

    data class Query(
        override val id: Long,
        val query: String,
        override val searchedAt: Instant,
    ) : RecentSearch

    data class PodcastSearch(
        override val id: Long,
        val podcastId: Long,
        val title: String,
        val imageUrl: String,
        val author: String,
        override val searchedAt: Instant,
    ) : RecentSearch

    data class EpisodeSearch(
        override val id: Long,
        val episodeId: Long,
        val title: String,
        val imageUrl: String,
        val feedTitle: String,
        override val searchedAt: Instant,
    ) : RecentSearch
}
