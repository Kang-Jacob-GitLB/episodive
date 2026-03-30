package io.jacob.episodive.core.database.util

import androidx.room.TypeConverter
import io.jacob.episodive.core.model.DownloadStatus
import io.jacob.episodive.core.model.toDownloadStatus

class DownloadStatusConverter {
    @TypeConverter
    fun fromDownloadStatus(status: DownloadStatus?): String? =
        status?.value

    @TypeConverter
    fun toDownloadStatus(value: String?): DownloadStatus? =
        value?.toDownloadStatus()
}
