package io.jacob.episodive.core.domain.usecase.episode

import io.jacob.episodive.core.domain.download.EpisodeDownloader
import io.jacob.episodive.core.domain.repository.EpisodeRepository
import io.jacob.episodive.core.model.Episode
import javax.inject.Inject

class SaveEpisodeUseCase @Inject constructor(
    private val episodeRepository: EpisodeRepository,
    private val episodeDownloader: EpisodeDownloader,
) {
    suspend operator fun invoke(episode: Episode): Boolean {
        episodeRepository.upsertEpisode(episode)
        val isSavedNow = episodeRepository.toggleSavedEpisode(episode)

        if (isSavedNow) {
            val ext = episode.enclosureType.substringAfterLast("/", "mp3")
            val filePath = "${episode.feedId}/${episode.id}.$ext"
            episodeDownloader.downloadEpisode(episode, filePath)
        } else {
            episode.filePath?.let { episodeDownloader.deleteDownloadedFile(it) }
        }

        return isSavedNow
    }
}
