package io.jacob.episodive.core.database.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import io.jacob.episodive.core.model.RecentSearchType
import kotlin.time.Instant

@Entity(
    tableName = "recent_searches",
    indices = [
        Index(value = ["type", "contentId"], unique = true),
        Index(value = ["type", "query"], unique = true),
    ]
)
data class RecentSearchEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: RecentSearchType,
    val query: String? = null,
    val contentId: Long? = null,
    val title: String? = null,
    val imageUrl: String? = null,
    val subtitle: String? = null,
    val searchedAt: Instant,
)
