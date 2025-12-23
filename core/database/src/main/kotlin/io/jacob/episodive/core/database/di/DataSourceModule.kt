package io.jacob.episodive.core.database.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.jacob.episodive.core.database.dao.EpisodeDao
import io.jacob.episodive.core.database.dao.PodcastDao
import io.jacob.episodive.core.database.dao.RecentSearchDao
import io.jacob.episodive.core.database.dao.SoundbiteDao
import io.jacob.episodive.core.database.datasource.EpisodeLocalDataSource
import io.jacob.episodive.core.database.datasource.EpisodeLocalDataSourceImpl
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