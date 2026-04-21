package io.jacob.episodive.core.domain.widget

interface WidgetDataReader {
    suspend fun snapshotNowPlaying(): NowPlayingSnapshot?
    suspend fun snapshotRecentEpisodes(limit: Int = 5): List<EpisodeSnapshot>
}
