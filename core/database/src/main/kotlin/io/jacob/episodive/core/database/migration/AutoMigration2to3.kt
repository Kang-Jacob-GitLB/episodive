package io.jacob.episodive.core.database.migration

import androidx.room.DeleteColumn
import androidx.room.migration.AutoMigrationSpec

@DeleteColumn.Entries(
    DeleteColumn(
        tableName = "episodes",
        columnName = "transcripts"
    )
)
class AutoMigration2to3 : AutoMigrationSpec