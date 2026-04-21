package io.jacob.episodive.core.domain.widget

data class EpisodeSnapshot(
    val id: Long,
    val podcastId: Long,
    val title: String,
    val feedTitle: String?,
    val imageUrl: String?,
    val duration: Long,
    val datePublished: Long,
)
