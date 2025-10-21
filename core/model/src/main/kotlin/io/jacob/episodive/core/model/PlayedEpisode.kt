package io.jacob.episodive.core.model

import io.jacob.episodive.core.model.mapper.toIntSeconds
import kotlin.time.Duration
import kotlin.time.Instant

data class PlayedEpisode(
    val episode: Episode,
    val playedAt: Instant,
    val position: Duration,
    val isCompleted: Boolean,
) {
    val progress: Float = episode.duration?.let {
        if (it == Duration.ZERO) 0f
        else position.toIntSeconds().toFloat() / it.toIntSeconds()
    } ?: 0f

    val remain: Duration? = episode.duration?.let {
        if (it == Duration.ZERO) null
        else it - position
    }
}
