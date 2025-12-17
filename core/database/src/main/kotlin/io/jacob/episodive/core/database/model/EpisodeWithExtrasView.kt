package io.jacob.episodive.core.database.model

import androidx.room.DatabaseView
import androidx.room.Embedded
import kotlin.time.Duration
import kotlin.time.Instant

@DatabaseView(
    viewName = "episode_with_extras",
    value = """
        SELECT
            episodes.*,
            liked_episodes.likedAt,
            played_episodes.playedAt,
            played_episodes.position,
            played_episodes.isCompleted
        FROM episodes
        LEFT JOIN liked_episodes ON episodes.id = liked_episodes.id
        LEFT JOIN played_episodes ON episodes.id = played_episodes.id
    """
)
data class EpisodeWithExtrasView(
    @Embedded val episode: EpisodeEntity,
    val likedAt: Instant? = null,
    val playedAt: Instant? = null,
    val position: Duration? = null,
    val isCompleted: Boolean? = null,
)
