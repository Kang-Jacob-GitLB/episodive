package io.jacob.episodive.core.database.migration

import androidx.room.DeleteColumn
import androidx.room.migration.AutoMigrationSpec

@DeleteColumn.Entries(
    DeleteColumn(
        tableName = "soundbites",
        columnName = "cacheKey"
    )
)
class AutoMigration5to6 : AutoMigrationSpec