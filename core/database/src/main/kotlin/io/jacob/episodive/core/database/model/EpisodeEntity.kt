package io.jacob.episodive.core.database.model

import androidx.room.Entity
import androidx.room.Index
import io.jacob.episodive.core.model.Category
import io.jacob.episodive.core.model.EpisodeType
import kotlin.time.Duration
import kotlin.time.Instant

@Entity(
    tableName = "episodes",
    primaryKeys = ["id", "cacheKey"],
    indices = [
        Index(value = ["id", "cachedAt"]),
        Index(value = ["title"]),
        Index(value = ["description"]),
        Index(value = ["feedAuthor"]),
    ],
)
data class EpisodeEntity(
    val id: Long,
    val title: String,
    val link: String,
    val description: String? = null,
    val guid: String,
    val datePublished: Instant,
    val dateCrawled: Instant,
    val enclosureUrl: String,
    val enclosureType: String,
    val enclosureLength: Long,
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
    val cacheKey: String,
    val cachedAt: Instant,
)
