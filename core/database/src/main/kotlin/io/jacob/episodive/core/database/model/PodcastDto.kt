package io.jacob.episodive.core.database.model

import androidx.room.Embedded
import kotlin.time.Instant

data class PodcastDto(
    @Embedded val podcast: PodcastEntity,
    val followedAt: Instant? = null,
    val isNotificationEnabled: Boolean? = null,
)
