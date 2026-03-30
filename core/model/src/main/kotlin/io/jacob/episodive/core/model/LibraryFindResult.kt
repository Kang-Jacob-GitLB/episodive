package io.jacob.episodive.core.model

data class LibraryFindResult(
    val playingEpisodes: List<Episode> = emptyList(),
    val likedEpisodes: List<Episode> = emptyList(),
    val savedEpisodes: List<Episode> = emptyList(),
    val followedPodcasts: List<Podcast> = emptyList(),
) {
    val isAllEmpty: Boolean
        get() = playingEpisodes.isEmpty() && likedEpisodes.isEmpty() && savedEpisodes.isEmpty() && followedPodcasts.isEmpty()
}