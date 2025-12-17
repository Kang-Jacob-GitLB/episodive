package io.jacob.episodive.core.database.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import kotlin.time.Instant

@Entity(
    tableName = "episode_group",
    primaryKeys = ["groupKey", "id"],
    foreignKeys = [
        ForeignKey(
            entity = EpisodeEntity::class,
            parentColumns = ["id"],
            childColumns = ["id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["id"]),
        Index(value = ["groupKey"]),
        Index(value = ["groupKey", "createdAt"]),
        Index(value = ["createdAt"]),
    ]
)
data class EpisodeGroupEntity(
    val groupKey: String,
    val id: Long,
    val order: Int,
    val createdAt: Instant,
)
