package io.jacob.episodive.core.database.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.jacob.episodive.core.database.dao.EpisodeDao
import io.jacob.episodive.core.database.dao.FeedDao
import io.jacob.episodive.core.database.dao.PodcastDao
import io.jacob.episodive.core.database.dao.RecentSearchDao
import io.jacob.episodive.core.database.dao.SoundbiteDao
import io.jacob.episodive.core.database.datasource.EpisodeLocalDataSource
import io.jacob.episodive.core.database.datasource.EpisodeLocalDataSourceImpl
import io.jacob.episodive.core.database.datasource.FeedLocalDataSource
import io.jacob.episodive.core.database.datasource.FeedLocalDataSourceImpl
import io.jacob.episodive.core.database.datasource.PodcastLocalDataSource
import io.jacob.episodive.core.database.datasource.PodcastLocalDataSourceImpl
import io.jacob.episodive.core.database.datasource.RecentSearchLocalDataSource
import io.jacob.episodive.core.database.datasource.RecentSearchLocalDataSourceImpl
import io.jacob.episodive.core.database.datasource.SoundbiteLocalDataSource
import io.jacob.episodive.core.database.datasource.SoundbiteLocalDataSourceImpl
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DataSourceModule {
    @Provides
    @Singleton
    fun providePodcastLocalDataSource(
        database: io.jacob.episodive.core.database.EpisodiveDatabase,
        podcastDao: PodcastDao,
    ): PodcastLocalDataSource {
        return PodcastLocalDataSourceImpl(
            database = database,
            podcastDao = podcastDao,
        )
    }

    @Provides
    @Singleton
    fun provideEpisodeLocalDataSource(
        database: io.jacob.episodive.core.database.EpisodiveDatabase,
        episodeDao: EpisodeDao,
    ): EpisodeLocalDataSource {
        return EpisodeLocalDataSourceImpl(
            database = database,
            episodeDao = episodeDao,
        )
    }

    @Provides
    @Singleton
    fun provideFeedLocalDataSource(
        feedDao: FeedDao,
    ): FeedLocalDataSource {
        return FeedLocalDataSourceImpl(
            feedDao = feedDao,
        )
    }

    @Provides
    @Singleton
    fun provideSoundbiteLocalDataSource(
        soundbiteDao: SoundbiteDao,
    ): SoundbiteLocalDataSource {
        return SoundbiteLocalDataSourceImpl(
            soundbiteDao = soundbiteDao,
        )
    }

    @Provides
    @Singleton
    fun provideRecentSearchLocalDataSource(
        recentSearchDao: RecentSearchDao,
    ): RecentSearchLocalDataSource {
        return RecentSearchLocalDataSourceImpl(
            recentSearchDao = recentSearchDao,
        )
    }
}