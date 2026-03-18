package io.jacob.episodive.core.model

data class LastPlayState(
    val episodeId: Long,
    val index: Int,
    val positionMs: Long,
    val shuffle: Boolean,
    val repeat: Repeat,
)
