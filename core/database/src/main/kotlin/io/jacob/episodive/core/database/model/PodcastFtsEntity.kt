package io.jacob.episodive.core.database.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Fts4
import androidx.room.PrimaryKey

@Fts4(contentEntity = PodcastEntity::class)
@Entity(tableName = "podcasts_fts")
data class PodcastFtsEntity(
    @PrimaryKey
    @ColumnInfo(name = "rowid")
    val id: Long,
    val title: String,
    val description: String,
    val author: String,
    val ownerName: String,
)
