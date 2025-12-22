package io.jacob.episodive.core.database.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Fts4
import androidx.room.PrimaryKey

@Fts4(contentEntity = EpisodeEntity::class)
@Entity(tableName = "episodes_fts")
data class EpisodeFtsEntity(
    @PrimaryKey
    @ColumnInfo(name = "rowid")
    val id: Long,
    val title: String,
    val description: String,
    val feedAuthor: String?,
    val feedTitle: String?,
)
