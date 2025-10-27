package io.jacob.episodive.core.model

data class FollowablePodcast(
    val podcast: Podcast,
    val isFollow: Boolean,
)