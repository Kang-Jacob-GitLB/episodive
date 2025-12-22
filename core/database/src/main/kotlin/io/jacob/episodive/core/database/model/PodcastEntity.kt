package io.jacob.episodive.core.database.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import io.jacob.episodive.core.model.Category
import io.jacob.episodive.core.model.Medium
import kotlin.time.Instant

@Entity(tableName = "podcasts")
data class PodcastEntity(
    @PrimaryKey val id: Long,
    val podcastGuid: String,
    val title: String,
    val url: String,
    val originalUrl: String,
    val link: String,
    val description: String,
    val author: String,
    val ownerName: String,
    val image: String,
    val artwork: String,
    val lastUpdateTime: Instant,
    val lastCrawlTime: Instant,
    val lastParseTime: Instant,
    val lastGoodHttpStatusTime: Instant,
    val lastHttpStatus: Int,
    val contentType: String,
    val itunesId: Long? = null,
    val itunesType: String? = null,
    val generator: String? = null,
    val language: String,
    val explicit: Boolean? = null,
    val type: Int,
    val medium: Medium? = null,
    val dead: Int,
    val chash: String? = null,
    val episodeCount: Int,
    val crawlErrors: Int,
    val parseErrors: Int,
    val categories: List<Category> = emptyList(),
    val locked: Int,
    val imageUrlHash: Long? = null,
    val newestItemPublishTime: Instant? = null,
)