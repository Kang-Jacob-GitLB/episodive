package io.jacob.episodive.core.domain.usecase.episode

import io.jacob.episodive.core.domain.repository.EpisodeRepository
import io.jacob.episodive.core.domain.repository.FeedRepository
import io.jacob.episodive.core.model.Episode
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
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
                // Create flows for each episode that continuously observe changes
                val allEpisodeFlows = mutableListOf<Flow<Episode>>()
                val chunks = limitedSoundbites.chunked(5)

                // Process each chunk
                chunks.forEachIndexed { index, chunk ->
                    // Create flows in parallel for this chunk
                    val chunkFlows = chunk.map { soundbite ->
                        async {
                            episodeRepository.getEpisodeById(soundbite.episodeId)
                                .filterNotNull()
                                .map { episode ->
                                    episode.copy(
                                        clipStartTime = soundbite.startTime,
                                        clipDuration = soundbite.duration,
                                    )
                                }
                        }
                    }.awaitAll()

                    allEpisodeFlows.addAll(chunkFlows)

                    val isLastChunk = index == chunks.lastIndex
                    if (isLastChunk) {
                        // Last chunk: collect continuously for real-time updates
                        combine(allEpisodeFlows) { episodes ->
                            episodes.toList()
                        }.collect { episodes ->
                            send(episodes)
                        }
                    } else {
                        // Not last chunk: emit once for progressive loading
                        val currentEpisodes = combine(allEpisodeFlows) { episodes ->
                            episodes.toList()
                        }.first()
                        send(currentEpisodes)
                    }
                }
            }
        }
    }
}
