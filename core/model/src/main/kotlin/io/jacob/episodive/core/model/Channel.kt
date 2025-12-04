package io.jacob.episodive.core.model

data class Channel(
    val id: Long,
    val title: String,
    val description: String,
    val image: String,
    val link: String,
    val count: Int,
    val podcastGuids: List<String>,
)
