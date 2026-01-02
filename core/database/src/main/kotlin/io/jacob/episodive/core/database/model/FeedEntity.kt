package io.jacob.episodive.core.database.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import io.jacob.episodive.core.model.Category
import kotlin.time.Instant

@Entity(tableName = "feeds")
data class FeedEntity(
    @PrimaryKey val id: Long,
    val url: String,
    val title: String,
    val newestItemPublishTime: Instant,
    val description: String? = null,
    val image: String? = null,
    val itunesId: Long? = null,
    val language: String,
    val categories: List<Category> = emptyList(),
    val groupKey: String,
    val cachedAt: Instant,
)
