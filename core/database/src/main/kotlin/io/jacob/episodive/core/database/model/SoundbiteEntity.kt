package io.jacob.episodive.core.database.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlin.time.Duration
import kotlin.time.Instant

@Entity(tableName = "soundbites")
data class SoundbiteEntity(
    val enclosureUrl: String,
    val title: String,
    val startTime: Instant,
    val duration: Duration,
    @PrimaryKey val episodeId: Long,
    val episodeTitle: String,
    val feedTitle: String,
    val feedUrl: String,
    val feedId: Long,
    val cachedAt: Instant,
)
