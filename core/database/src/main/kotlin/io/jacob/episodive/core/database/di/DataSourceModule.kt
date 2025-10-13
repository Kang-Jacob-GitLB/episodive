package io.jacob.episodive.core.database.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.jacob.episodive.core.database.dao.EpisodeDao
import io.jacob.episodive.core.database.dao.FeedDao
import io.jacob.episodive.core.database.dao.PodcastDao
import io.jacob.episodive.core.database.dao.RecentSearchDao
import io.jacob.episodive.core.database.datasource.EpisodeLocalDataSource
import io.jacob.episodive.core.database.datasource.EpisodeLocalDataSourceImpl
import io.jacob.episodive.core.database.datasource.FeedLocalDataSource
import io.jacob.episodive.core.database.datasource.FeedLocalDataSourceImpl
import io.jacob.episodive.core.database.datasource.PodcastLocalDataSource
import io.jacob.episodive.core.database.datasource.PodcastLocalDataSourceImpl
import io.jacob.episodive.core.database.datasource.RecentSearchLocalDataSource
import io.jacob.episodive.core.database.datasource.RecentSearchLocalDataSourceImpl
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DataSourceModule {
    @Provides
    @Singleton
    fun providePodcastLocalDataSource(
        podcastDao: PodcastDao,
    ): PodcastLocalDataSource {
        return PodcastLocalDataSourceImpl(
            podcastDao = podcastDao,
        )
    }

    @Provides
    @Singleton
    fun provideEpisodeLocalDataSource(
        episodeDao: EpisodeDao,
    ): EpisodeLocalDataSource {
        return EpisodeLocalDataSourceImpl(
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
    fun provideRecentSearchLocalDataSource(
        recentSearchDao: RecentSearchDao,
    ): RecentSearchLocalDataSource {
        return RecentSearchLocalDataSourceImpl(
            recentSearchDao = recentSearchDao,
        )
    }
}