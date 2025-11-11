package io.jacob.episodive.core.database.model

import androidx.room.Embedded
import kotlin.time.Duration
import kotlin.time.Instant

data class EpisodeDto(
    @Embedded val episode: EpisodeEntity,
    val likedAt: Instant? = null,
    val playedAt: Instant? = null,
    val position: Duration? = null,
    val isCompleted: Boolean? = null,
)
