package io.jacob.episodive.core.model

import kotlin.time.Duration

data class Chapter(
    val title: String,
    val startTime: Duration,
    val endTime: Duration,
)