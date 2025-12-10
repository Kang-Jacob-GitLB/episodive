package io.jacob.episodive.core.domain.usecase

import io.jacob.episodive.core.domain.repository.EpisodeRepository
import io.jacob.episodive.core.domain.repository.PodcastRepository
import io.jacob.episodive.core.model.LibraryFindResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

class FindInLibraryUseCase @Inject constructor(
    private val podcastRepository: PodcastRepository,
    private val episodeRepository: EpisodeRepository,
) {
    operator fun invoke(query: String): Flow<LibraryFindResult> {
        return combine(
            episodeRepository.getPlayingEpisodes(query, 1000),
            episodeRepository.getLikedEpisodes(query, 1000),
            podcastRepository.getFollowedPodcasts(query, 1000),
        ) { playingEpisodes, likedEpisodes, followedPodcasts ->
            LibraryFindResult(
                playingEpisodes = playingEpisodes,
                likedEpisodes = likedEpisodes,
                followedPodcasts = followedPodcasts,
            )
        }
    }
}