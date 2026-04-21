package io.jacob.episodive.core.domain.widget

data class NowPlayingSnapshot(
    val episodeId: Long,
    val podcastId: Long,
    val title: String,
    val feedTitle: String?,
    val imageUrl: String?,
    val isPlaying: Boolean,
)
