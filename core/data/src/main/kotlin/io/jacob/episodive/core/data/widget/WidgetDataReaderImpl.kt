package io.jacob.episodive.core.data.widget

import io.jacob.episodive.core.common.EpisodivePlayers
import io.jacob.episodive.core.common.Player
import io.jacob.episodive.core.domain.repository.PlayerRepository
import io.jacob.episodive.core.domain.usecase.episode.GetRecentEpisodesUseCase
import io.jacob.episodive.core.domain.usecase.player.GetNowPlayingUseCase
import io.jacob.episodive.core.domain.widget.EpisodeSnapshot
import io.jacob.episodive.core.domain.widget.NowPlayingSnapshot
import io.jacob.episodive.core.domain.widget.WidgetDataReader
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WidgetDataReaderImpl @Inject constructor(
    private val getNowPlaying: GetNowPlayingUseCase,
    private val getRecentEpisodes: GetRecentEpisodesUseCase,
    @param:Player(EpisodivePlayers.Main) private val playerRepository: PlayerRepository,
) : WidgetDataReader {

    override suspend fun snapshotNowPlaying(): NowPlayingSnapshot? {
        val episode = getNowPlaying().first() ?: return null
        val isPlaying = playerRepository.isPlaying.first()
        return NowPlayingSnapshot(
            episodeId = episode.id,
            podcastId = episode.feedId,
            title = episode.title,
            feedTitle = episode.feedTitle,
            // 에피소드 자체 image 가 비어있으면 feed(팟캐스트) 이미지로 fallback.
            imageUrl = episode.image.ifBlank { episode.feedImage }.ifBlank { null },
            isPlaying = isPlaying,
        )
    }

    override suspend fun snapshotRecentEpisodes(limit: Int): List<EpisodeSnapshot> =
        getRecentEpisodes(limit).first().map { episode ->
            EpisodeSnapshot(
                id = episode.id,
                podcastId = episode.feedId,
                title = episode.title,
                feedTitle = episode.feedTitle,
                imageUrl = episode.image.ifBlank { episode.feedImage }.ifBlank { null },
                duration = episode.duration?.inWholeMilliseconds ?: 0L,
                datePublished = episode.datePublished.toEpochMilliseconds(),
            )
        }
}
