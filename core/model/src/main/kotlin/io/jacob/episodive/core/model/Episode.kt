package io.jacob.episodive.core.model

import io.jacob.episodive.core.model.mapper.toIntSeconds
import kotlin.time.Duration
import kotlin.time.Instant

data class Episode(
    val id: Long,
    val title: String,
    val link: String,
    val description: String? = null,
    val guid: String,
    val datePublished: Instant,
    val dateCrawled: Instant,
    val enclosureUrl: String,
    val enclosureType: String,
    val enclosureLength: Long, // in bytes, 0 is live
    val startTime: Instant? = null, // for live episodes
    val endTime: Instant? = null, // for live episodes
    val status: String? = null, // for live episodes
    val contentLink: String? = null, // for live episodes
    val duration: Duration? = null,
    val explicit: Boolean,
    val episode: Int? = null,
    val episodeType: EpisodeType? = null,
    val season: Int? = null,
    val image: String,
    val feedItunesId: Long? = null,
    val feedImage: String,
    val feedId: Long,
    val feedUrl: String? = null,
    val feedAuthor: String? = null,
    val feedTitle: String? = null,
    val feedLanguage: String,
    val categories: List<Category> = emptyList(),
    val chaptersUrl: String? = null,
    val transcriptUrl: String? = null,
    val likedAt: Instant? = null,
    val playedAt: Instant? = null,
    val position: Duration = Duration.ZERO,
    val isCompleted: Boolean = false,
    val clipStartTime: Instant? = null, // for soundbite
    val clipDuration: Duration? = null, // for soundbite
) {
    val isLive: Boolean
        get() = enclosureLength == 0L ||
                startTime != null ||
                endTime != null ||
                status != null ||
                contentLink != null

    val isLiked: Boolean = likedAt != null

    val progress: Float = duration?.let {
        if (it == Duration.ZERO) 0f
        else position.toIntSeconds().toFloat() / it.toIntSeconds()
    } ?: 0f

    val remain: Duration? = duration?.let {
        if (it == Duration.ZERO) null
        else it - position
    }

    val clipStartPositionMs: Long = clipStartTime?.toEpochMilliseconds() ?: 0L
    val clipEndPositionMs: Long = clipStartPositionMs + (clipDuration?.inWholeMilliseconds ?: 0L)
    val hasClip: Boolean = clipStartTime != null && clipDuration != null
}
