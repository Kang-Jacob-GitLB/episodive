package io.jacob.episodive.core.database.migration

import androidx.room.DeleteTable
import androidx.room.migration.AutoMigrationSpec

@DeleteTable.Entries(
    DeleteTable(
        tableName = "trending_feeds"
    ),
    DeleteTable(
        tableName = "recent_feeds"
    ),
    DeleteTable(
        tableName = "recent_new_feeds"
    )
)
class AutoMigration6to7 : AutoMigrationSpec