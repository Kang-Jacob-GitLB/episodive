package io.jacob.episodive.core.database.model

import androidx.room.DatabaseView
import androidx.room.Embedded
import io.jacob.episodive.core.model.DownloadStatus
import kotlin.time.Duration
import kotlin.time.Instant

@DatabaseView(
    viewName = "episode_with_extras",
    value = """
        SELECT
            episodes.*,
            liked_episodes.likedAt AS likedAt,
            played_episodes.playedAt AS playedAt,
            played_episodes.position AS position,
            played_episodes.isCompleted AS isCompleted,
            soundbites.startTime AS clipStartTime,
            soundbites.duration AS clipDuration,
            saved_episodes.savedAt AS savedAt,
            saved_episodes.filePath AS filePath,
            saved_episodes.downloadStatus AS downloadStatus
        FROM episodes
        LEFT JOIN liked_episodes ON episodes.id = liked_episodes.id
        LEFT JOIN played_episodes ON episodes.id = played_episodes.id
        LEFT JOIN soundbites ON episodes.id = soundbites.episodeId
        LEFT JOIN saved_episodes ON episodes.id = saved_episodes.id
    """
)
data class EpisodeWithExtrasView(
    @Embedded val episode: EpisodeEntity,
    val likedAt: Instant? = null,
    val playedAt: Instant? = null,
    val position: Duration? = null,
    val isCompleted: Boolean? = null,
    val clipStartTime: Instant? = null,
    val clipDuration: Duration? = null,
    val savedAt: Instant? = null,
    val filePath: String? = null,
    val downloadStatus: DownloadStatus? = null,
)
