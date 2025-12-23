package io.jacob.episodive.core.database

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import io.jacob.episodive.core.database.dao.EpisodeDao
import io.jacob.episodive.core.database.dao.PodcastDao
import io.jacob.episodive.core.database.dao.RecentSearchDao
import io.jacob.episodive.core.database.dao.SoundbiteDao
import io.jacob.episodive.core.database.migration.AutoMigration2to3
import io.jacob.episodive.core.database.migration.AutoMigration3to4
import io.jacob.episodive.core.database.migration.AutoMigration4to5
import io.jacob.episodive.core.database.migration.AutoMigration5to6
import io.jacob.episodive.core.database.migration.AutoMigration6to7
import io.jacob.episodive.core.database.model.EpisodeEntity
import io.jacob.episodive.core.database.model.EpisodeFtsEntity
import io.jacob.episodive.core.database.model.EpisodeGroupEntity
import io.jacob.episodive.core.database.model.EpisodeWithExtrasView
import io.jacob.episodive.core.database.model.FollowedPodcastEntity
import io.jacob.episodive.core.database.model.LikedEpisodeEntity
import io.jacob.episodive.core.database.model.PlayedEpisodeEntity
import io.jacob.episodive.core.database.model.PodcastEntity
import io.jacob.episodive.core.database.model.PodcastFtsEntity
import io.jacob.episodive.core.database.model.PodcastGroupEntity
import io.jacob.episodive.core.database.model.PodcastWithExtrasView
import io.jacob.episodive.core.database.model.RecentSearchEntity
import io.jacob.episodive.core.database.model.SoundbiteEntity
import io.jacob.episodive.core.database.util.CategoryConverter
import io.jacob.episodive.core.database.util.DurationConverter
import io.jacob.episodive.core.database.util.EpisodeTypeConverter
import io.jacob.episodive.core.database.util.InstantConverter
import io.jacob.episodive.core.database.util.MediumConverter

@Database(
    entities = [
        PodcastEntity::class,
        PodcastFtsEntity::class,
        PodcastGroupEntity::class,
        FollowedPodcastEntity::class,
        EpisodeEntity::class,
        EpisodeFtsEntity::class,
        EpisodeGroupEntity::class,
        LikedEpisodeEntity::class,
        PlayedEpisodeEntity::class,
        SoundbiteEntity::class,
        RecentSearchEntity::class,
    ],
    views = [
        PodcastWithExtrasView::class,
        EpisodeWithExtrasView::class,
    ],
    version = 7,
    autoMigrations = [
        AutoMigration(from = 1, to = 2),
        AutoMigration(from = 2, to = 3, spec = AutoMigration2to3::class),
        AutoMigration(from = 3, to = 4, spec = AutoMigration3to4::class),
        AutoMigration(from = 4, to = 5, spec = AutoMigration4to5::class),
        AutoMigration(from = 5, to = 6, spec = AutoMigration5to6::class),
        AutoMigration(from = 6, to = 7, spec = AutoMigration6to7::class),
    ],
    exportSchema = true
)
@TypeConverters(
    CategoryConverter::class,
    DurationConverter::class,
    EpisodeTypeConverter::class,
    InstantConverter::class,
    MediumConverter::class,
)
abstract class EpisodiveDatabase : RoomDatabase() {
    abstract fun podcastDao(): PodcastDao
    abstract fun episodeDao(): EpisodeDao
    abstract fun soundbiteDao(): SoundbiteDao
    abstract fun recentSearchDao(): RecentSearchDao
}