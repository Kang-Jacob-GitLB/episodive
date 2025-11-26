package io.jacob.episodive.core.model

import io.jacob.episodive.core.model.mapper.toIntSeconds
import kotlin.time.Duration

data class Progress(
    val position: Duration,
    val buffered: Duration,
    val duration: Duration,
) {
    val positionRatio: Float =
        if (duration.isPositive() && duration.toIntSeconds() > 0) {
            (position.toIntSeconds().toFloat() / duration.toIntSeconds()).coerceIn(0f, 1f)
        } else {
            0f
        }

    val bufferedRatio: Float =
        if (duration.isPositive() && duration.toIntSeconds() > 0) {
            (buffered.toIntSeconds().toFloat() / duration.toIntSeconds()).coerceIn(0f, 1f)
        } else {
            0f
        }

    val remaining: Duration = (duration - position).coerceAtLeast(Duration.ZERO)
    val remainingRatio: Float =
        if (duration.isPositive() && duration.toIntSeconds() > 0) {
            (remaining.toIntSeconds().toFloat() / duration.toIntSeconds()).coerceIn(0f, 1f)
        } else {
            0f
        }
}