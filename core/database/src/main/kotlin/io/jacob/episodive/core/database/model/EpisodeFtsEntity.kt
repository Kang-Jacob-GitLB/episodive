package io.jacob.episodive.core.database.model

import androidx.room.Entity
import androidx.room.Fts4

@Fts4(contentEntity = EpisodeEntity::class)
@Entity(tableName = "episodes_fts")
data class EpisodeFtsEntity(
    val title: String,
    val description: String,
    val feedAuthor: String?,
    val feedTitle: String?,
)
