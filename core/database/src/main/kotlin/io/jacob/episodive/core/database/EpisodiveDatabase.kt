package io.jacob.episodive.core.database

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import io.jacob.episodive.core.database.dao.EpisodeDao
import io.jacob.episodive.core.database.dao.FeedDao
import io.jacob.episodive.core.database.dao.PodcastDao
import io.jacob.episodive.core.database.dao.RecentSearchDao
import io.jacob.episodive.core.database.model.EpisodeEntity
import io.jacob.episodive.core.database.model.FollowedPodcastEntity
import io.jacob.episodive.core.database.model.LikedEpisodeEntity
import io.jacob.episodive.core.database.model.PlayedEpisodeEntity
import io.jacob.episodive.core.database.model.PodcastEntity
import io.jacob.episodive.core.database.model.RecentFeedEntity
import io.jacob.episodive.core.database.model.RecentNewFeedEntity
import io.jacob.episodive.core.database.model.RecentSearchEntity
import io.jacob.episodive.core.database.model.SoundbiteEntity
import io.jacob.episodive.core.database.model.TrendingFeedEntity
import io.jacob.episodive.core.database.util.CategoryConverter
import io.jacob.episodive.core.database.util.DurationConverter
import io.jacob.episodive.core.database.util.EpisodeTypeConverter
import io.jacob.episodive.core.database.util.InstantConverter
import io.jacob.episodive.core.database.util.MediumConverter
import io.jacob.episodive.core.database.util.SoundbiteConverter
import io.jacob.episodive.core.database.util.TranscriptConverter

@Database(
    entities = [
        PodcastEntity::class,
        FollowedPodcastEntity::class,
        EpisodeEntity::class,
        LikedEpisodeEntity::class,
        PlayedEpisodeEntity::class,
        TrendingFeedEntity::class,
        RecentFeedEntity::class,
        RecentNewFeedEntity::class,
        SoundbiteEntity::class,
        RecentSearchEntity::class,
    ],
    version = 2,
    autoMigrations = [
        AutoMigration(from = 1, to = 2),
    ],
    exportSchema = true
)
@TypeConverters(
    CategoryConverter::class,
    DurationConverter::class,
    EpisodeTypeConverter::class,
    InstantConverter::class,
    MediumConverter::class,
    SoundbiteConverter::class,
    TranscriptConverter::class,
)
abstract class EpisodiveDatabase : RoomDatabase() {
    abstract fun podcastDao(): PodcastDao
    abstract fun episodeDao(): EpisodeDao
    abstract fun feedDao(): FeedDao
    abstract fun recentSearchDao(): RecentSearchDao
}