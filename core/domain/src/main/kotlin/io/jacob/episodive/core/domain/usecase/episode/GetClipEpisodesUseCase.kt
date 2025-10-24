package io.jacob.episodive.core.domain.usecase.episode

import io.jacob.episodive.core.domain.repository.EpisodeRepository
import io.jacob.episodive.core.domain.repository.FeedRepository
import io.jacob.episodive.core.model.ClipEpisode
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
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

                limitedSoundbites.chunked(5).forEach { chunk ->
                    // Fetch 5 episodes in parallel while maintaining order
                    val chunkClipEpisodes = coroutineScope {
                        chunk.map { soundbite ->
                            async {
                                episodeRepository.getEpisodeById(soundbite.episodeId).first()
                                    ?.let { episode ->
                                        ClipEpisode(
                                            episode = episode,
                                            clipStartTime = soundbite.startTime,
                                            clipDuration = soundbite.duration,
                                        )
                                    }
                            }
                        }.awaitAll().filterNotNull()
                    }
                    allClipEpisodes.addAll(chunkClipEpisodes)
                    emit(allClipEpisodes.toList())
                }
            }
        }
    }
}
