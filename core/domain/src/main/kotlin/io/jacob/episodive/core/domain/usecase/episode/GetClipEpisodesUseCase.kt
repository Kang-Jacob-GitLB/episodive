package io.jacob.episodive.core.domain.usecase.episode

import io.jacob.episodive.core.domain.repository.EpisodeRepository
import io.jacob.episodive.core.domain.repository.FeedRepository
import io.jacob.episodive.core.model.ClipEpisode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

class GetClipEpisodesUseCase @Inject constructor(
    private val feedRepository: FeedRepository,
    private val episodeRepository: EpisodeRepository
) {
    operator fun invoke(max: Int = 20): Flow<List<ClipEpisode>> {
        return feedRepository.getRecentSoundbites().flatMapLatest { soundbites ->
            val limitedSoundbites = soundbites.take(max)
            Timber.i("size of soundbites: ${limitedSoundbites.size}")

            if (limitedSoundbites.isEmpty()) {
                return@flatMapLatest flowOf(emptyList())
            }

            channelFlow {
                // Map to store all clip episodes by their index
                val clipEpisodesMap = mutableMapOf<Int, ClipEpisode>()
                var globalIndex = 0

                limitedSoundbites.chunked(5).forEach { chunk ->
                    // For each chunk, start collecting all episodes in parallel
                    chunk.forEach { soundbite ->
                        val currentIndex = globalIndex++
                        launch {
                            episodeRepository.getEpisodeById(soundbite.episodeId)
                                .filterNotNull()
                                .collect { episode ->
                                    clipEpisodesMap[currentIndex] = ClipEpisode(
                                        episode = episode,
                                        clipStartTime = soundbite.startTime,
                                        clipDuration = soundbite.duration,
                                    )
                                    // Emit current accumulated state in order
                                    val sortedList = clipEpisodesMap.entries
                                        .sortedBy { it.key }
                                        .map { it.value }
                                    send(sortedList)
                                }
                        }
                    }
                }
            }
        }
    }
}
