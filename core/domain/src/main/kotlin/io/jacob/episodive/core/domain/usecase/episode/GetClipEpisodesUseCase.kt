package io.jacob.episodive.core.domain.usecase.episode

import io.jacob.episodive.core.domain.repository.EpisodeRepository
import io.jacob.episodive.core.domain.repository.FeedRepository
import io.jacob.episodive.core.model.Episode
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import timber.log.Timber
import javax.inject.Inject

class GetClipEpisodesUseCase @Inject constructor(
    private val feedRepository: FeedRepository,
    private val episodeRepository: EpisodeRepository
) {
    operator fun invoke(max: Int = 20): Flow<List<Episode>> {
        return feedRepository.getRecentSoundbites().flatMapLatest { soundbites ->
            val limitedSoundbites = soundbites.take(max)
            Timber.i("size of soundbites: ${limitedSoundbites.size}")

            if (limitedSoundbites.isEmpty()) {
                return@flatMapLatest flowOf(emptyList())
            }

            channelFlow {
                val allEpisodes = mutableListOf<Episode>()

                limitedSoundbites.chunked(5).forEach { chunk ->
                    // Process each chunk in parallel using async
                    val deferredEpisodes = chunk.map { soundbite ->
                        async {
                            episodeRepository.getEpisodeById(soundbite.episodeId)
                                .filterNotNull()
                                .first()
                                .copy(
                                    clipStartTime = soundbite.startTime,
                                    clipDuration = soundbite.duration,
                                )
                        }
                    }

                    // Wait for all episodes in chunk to complete
                    val chunkEpisodes = deferredEpisodes.awaitAll()
                    allEpisodes.addAll(chunkEpisodes)

                    // Emit accumulated episodes so far
                    send(allEpisodes.toList())
                }
            }
        }
    }
}
