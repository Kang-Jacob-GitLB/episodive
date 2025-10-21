package io.jacob.episodive.core.model

data class LibraryFindResult(
    val playingEpisodes: List<PlayedEpisode> = emptyList(),
    val likedEpisodes: List<LikedEpisode> = emptyList(),
    val followedPodcasts: List<FollowedPodcast> = emptyList(),
) {
    val isAllEmpty: Boolean
        get() = playingEpisodes.isEmpty() && likedEpisodes.isEmpty() && followedPodcasts.isEmpty()
}