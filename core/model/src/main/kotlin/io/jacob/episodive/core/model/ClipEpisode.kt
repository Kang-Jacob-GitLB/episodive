package io.jacob.episodive.core.model

import kotlin.time.Duration
import kotlin.time.Instant

data class ClipEpisode(
    val episode: Episode,
    val clipStartTime: Instant,
    val clipDuration: Duration,
)
