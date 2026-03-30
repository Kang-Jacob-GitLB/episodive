package io.jacob.episodive.core.database.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import io.jacob.episodive.core.model.DownloadStatus
import kotlin.time.Instant

@Entity(
    tableName = "saved_episodes",
    foreignKeys = [
        ForeignKey(
            entity = EpisodeEntity::class,
            parentColumns = ["id"],
            childColumns = ["id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["savedAt"]),
    ]
)
data class SavedEpisodeEntity(
    @PrimaryKey val id: Long,
    val podcastId: Long,
    val savedAt: Instant,
    val filePath: String,
    val totalSize: Long,
    val downloadedSize: Long,
    val downloadStatus: DownloadStatus,
)
