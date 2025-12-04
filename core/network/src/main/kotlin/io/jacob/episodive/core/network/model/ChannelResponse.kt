package io.jacob.episodive.core.network.model

import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.Serializable

@OptIn(InternalSerializationApi::class)
@Serializable
data class ChannelResponse(
    val id: Long,
    val title: String,
    val description: String,
    val image: String,
    val link: String,
    val count: Int,
    val podcastGuids: List<String>,
)
