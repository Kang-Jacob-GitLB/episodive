package io.jacob.episodive.core.database.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.jacob.episodive.core.database.EpisodiveDatabase
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DaoModule {
    @Provides
    @Singleton
    fun providePodcastDao(database: EpisodiveDatabase) = database.podcastDao()

    @Provides
    @Singleton
    fun provideEpisodeDao(database: EpisodiveDatabase) = database.episodeDao()

    @Provides
    @Singleton
    fun provideFeedDao(database: EpisodiveDatabase) = database.feedDao()

    @Provides
    @Singleton
    fun provideRecentSearchDao(database: EpisodiveDatabase) = database.recentSearchDao()
}