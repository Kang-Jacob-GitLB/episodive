package io.jacob.episodive.core.database.migration

import androidx.room.DeleteColumn
import androidx.room.migration.AutoMigrationSpec

@DeleteColumn.Entries(
    DeleteColumn(
        tableName = "podcasts",
        columnName = "cacheKey"
    ),
    DeleteColumn(
        tableName = "podcasts",
        columnName = "cachedAt"
    )
)
class AutoMigration4to5 : AutoMigrationSpec