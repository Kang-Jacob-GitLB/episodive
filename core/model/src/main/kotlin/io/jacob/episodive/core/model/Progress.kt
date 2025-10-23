package io.jacob.episodive.core.model

import io.jacob.episodive.core.model.mapper.toIntSeconds
import kotlin.time.Duration

data class Progress(
    val position: Duration,
    val buffered: Duration,
    val duration: Duration,
) {
    val positionRatio: Float =
        if (duration == Duration.ZERO) 0f
        else position.toIntSeconds().toFloat() / duration.toIntSeconds()
    val bufferedRatio: Float =
        if (duration == Duration.ZERO) 0f
        else buffered.toIntSeconds().toFloat() / duration.toIntSeconds()

    val remaining: Duration = duration - position
    val remainingRatio: Float =
        if (duration == Duration.ZERO) 0f
        else remaining.toIntSeconds().toFloat() / duration.toIntSeconds()
}