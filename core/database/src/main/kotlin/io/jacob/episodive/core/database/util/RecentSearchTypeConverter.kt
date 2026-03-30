package io.jacob.episodive.core.database.util

import androidx.room.TypeConverter
import io.jacob.episodive.core.model.RecentSearchType
import io.jacob.episodive.core.model.mapper.toRecentSearchType
import io.jacob.episodive.core.model.mapper.toValue

class RecentSearchTypeConverter {
    @TypeConverter
    fun fromRecentSearchType(type: RecentSearchType?): String? =
        type?.toValue()

    @TypeConverter
    fun toRecentSearchType(value: String?): RecentSearchType? =
        value?.toRecentSearchType()
}
