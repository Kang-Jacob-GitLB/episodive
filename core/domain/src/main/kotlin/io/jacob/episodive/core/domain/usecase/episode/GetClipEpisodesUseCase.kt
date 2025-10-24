package io.jacob.episodive.core.domain.usecase.episode

import io.jacob.episodive.core.domain.repository.EpisodeRepository
import io.jacob.episodive.core.domain.repository.FeedRepository
import io.jacob.episodive.core.model.ClipEpisode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import timber.log.Timber
import javax.inject.Inject

class GetClipEpisodesUseCase @Inject constructor(
    private val feedRepository: FeedRepository,
    private val episodeRepository: EpisodeRepository
) {
    @Suppress("UnusedFlow")
    operator fun invoke(max: Int = 20): Flow<List<ClipEpisode>> {
        return feedRepository.getRecentSoundbites().flatMapLatest { soundbites ->
            val limitedSoundbites = soundbites.take(max)
            Timber.i("size of soundbites: ${limitedSoundbites.size}")

            if (limitedSoundbites.isEmpty()) {
                return@flatMapLatest flowOf(emptyList())
            }

            flow {
                val allClipEpisodes = mutableListOf<ClipEpisode>()

                limitedSoundbites.forEachIndexed { index, soundbite ->
                    episodeRepository.getEpisodeById(soundbite.episodeId).first()?.let { episode ->
                        val clipEpisode = ClipEpisode(
                            episode = episode,
                            clipStartTime = soundbite.startTime,
                            clipDuration = soundbite.duration,
                        )
                        allClipEpisodes.add(clipEpisode)
                        emit(allClipEpisodes.toList())
                    }
                }
            }
        }
    }
}