package io.jacob.episodive.core.database.model

import androidx.room.DatabaseView
import androidx.room.Embedded
import kotlin.time.Instant

@DatabaseView(
    viewName = "podcast_with_extras",
    value = """
        SELECT
            podcasts.*,
            followed_podcasts.followedAt AS followedAt,
            followed_podcasts.isNotificationEnabled AS isNotificationEnabled
        FROM podcasts
        LEFT JOIN followed_podcasts ON podcasts.id = followed_podcasts.id
    """
)
data class PodcastWithExtrasView(
    @Embedded val podcast: PodcastEntity,
    val followedAt: Instant? = null,
    val isNotificationEnabled: Boolean? = null,
)
