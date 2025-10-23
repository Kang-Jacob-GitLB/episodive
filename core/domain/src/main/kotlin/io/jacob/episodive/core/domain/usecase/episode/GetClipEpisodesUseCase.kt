package io.jacob.episodive.core.domain.usecase.episode

import io.jacob.episodive.core.domain.repository.EpisodeRepository
import io.jacob.episodive.core.domain.repository.FeedRepository
import io.jacob.episodive.core.model.ClipEpisode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import timber.log.Timber
import javax.inject.Inject

class GetClipEpisodesUseCase @Inject constructor(
    private val feedRepository: FeedRepository,
    private val episodeRepository: EpisodeRepository
) {
    @Suppress("UnusedFlow")
    operator fun invoke(max: Int = 20): Flow<List<ClipEpisode>> {
        return feedRepository.getRecentSoundbites().flatMapLatest {
            val soundbites = it.take(max)
            Timber.i("size of soundbites: ${soundbites.size}")
            if (soundbites.isEmpty()) {
                return@flatMapLatest flowOf(emptyList())
            }

            val episodeFlows = soundbites.map { soundbite ->
                Timber.i("episode: ${soundbite.episodeId}")
                episodeRepository.getEpisodeById(soundbite.episodeId)
            }

            combine(episodeFlows) { episodes ->
                episodes.mapIndexedNotNull { index, episode ->
                    episode?.let { nonNullEpisode ->
                        ClipEpisode(
                            episode = nonNullEpisode,
                            clipStartTime = soundbites[index].startTime,
                            clipDuration = soundbites[index].duration,
                        )
                    }
                }
            }
        }
    }
}