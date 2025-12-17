package io.jacob.episodive.core.database.migration

import androidx.room.DeleteColumn
import androidx.room.migration.AutoMigrationSpec

@DeleteColumn.Entries(
    DeleteColumn(
        tableName = "episodes",
        columnName = "cacheKey"
    ),
    DeleteColumn(
        tableName = "episodes",
        columnName = "cachedAt"
    )
)
class AutoMigration3to4 : AutoMigrationSpec