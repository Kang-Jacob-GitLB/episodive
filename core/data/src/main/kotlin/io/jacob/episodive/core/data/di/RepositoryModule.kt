package io.jacob.episodive.core.data.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.jacob.episodive.core.data.repository.EpisodeRepositoryImpl
import io.jacob.episodive.core.data.repository.FeedRepositoryImpl
import io.jacob.episodive.core.data.repository.ImageRepositoryImpl
import io.jacob.episodive.core.data.repository.PlayerRepositoryImpl
import io.jacob.episodive.core.data.repository.PodcastRepositoryImpl
import io.jacob.episodive.core.data.repository.RecentSearchRepositoryImpl
import io.jacob.episodive.core.data.repository.UserRepositoryImpl
import io.jacob.episodive.core.data.util.updater.EpisodeRemoteUpdater
import io.jacob.episodive.core.data.util.updater.PodcastRemoteUpdater
import io.jacob.episodive.core.database.datasource.EpisodeLocalDataSource
import io.jacob.episodive.core.database.datasource.FeedLocalDataSource
import io.jacob.episodive.core.database.datasource.PodcastLocalDataSource
import io.jacob.episodive.core.database.datasource.RecentSearchLocalDataSource
import io.jacob.episodive.core.datastore.datasource.UserPreferencesDataSource
import io.jacob.episodive.core.domain.di.ClipPlayerRepository
import io.jacob.episodive.core.domain.di.MainPlayerRepository
import io.jacob.episodive.core.domain.repository.EpisodeRepository
import io.jacob.episodive.core.domain.repository.FeedRepository
import io.jacob.episodive.core.domain.repository.ImageRepository
import io.jacob.episodive.core.domain.repository.PlayerRepository
import io.jacob.episodive.core.domain.repository.PodcastRepository
import io.jacob.episodive.core.domain.repository.RecentSearchRepository
import io.jacob.episodive.core.domain.repository.UserRepository
import io.jacob.episodive.core.network.datasource.EpisodeRemoteDataSource
import io.jacob.episodive.core.network.datasource.FeedRemoteDataSource
import io.jacob.episodive.core.network.datasource.PodcastRemoteDataSource
import io.jacob.episodive.core.player.datasource.PlayerDataSource
import io.jacob.episodive.core.player.di.ClipPlayerDataSource
import io.jacob.episodive.core.player.di.MainPlayerDataSource
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {
    @Provides
    @Singleton
    fun providePodcastRepository(
        podcastLocalDataSource: PodcastLocalDataSource,
        podcastRemoteDataSource: PodcastRemoteDataSource,
        podcastRemoteUpdater: PodcastRemoteUpdater.Factory,
    ): PodcastRepository {
        return PodcastRepositoryImpl(
            localDataSource = podcastLocalDataSource,
            remoteDataSource = podcastRemoteDataSource,
            remoteUpdater = podcastRemoteUpdater,
        )
    }

    @Provides
    @Singleton
    fun provideEpisodeRepository(
        episodeLocalDataSource: EpisodeLocalDataSource,
        episodeRemoteDataSource: EpisodeRemoteDataSource,
        episodeRemoteUpdater: EpisodeRemoteUpdater.Factory,
    ): EpisodeRepository {
        return EpisodeRepositoryImpl(
            localDataSource = episodeLocalDataSource,
            remoteDataSource = episodeRemoteDataSource,
            remoteUpdater = episodeRemoteUpdater,
        )
    }

    @Provides
    @Singleton
    fun provideFeedRepository(
        feedLocalDataSource: FeedLocalDataSource,
        feedRemoteDataSource: FeedRemoteDataSource,
    ): FeedRepository {
        return FeedRepositoryImpl(
            localDataSource = feedLocalDataSource,
            remoteDataSource = feedRemoteDataSource,
        )
    }

    @Provides
    @Singleton
    fun provideUserRepository(
        userPreferencesDataSource: UserPreferencesDataSource,
    ): UserRepository {
        return UserRepositoryImpl(
            userPreferencesDataSource = userPreferencesDataSource,
        )
    }

    @Provides
    @Singleton
    @MainPlayerRepository
    fun provideMainPlayerRepository(
        @MainPlayerDataSource mainPlayerDataSource: PlayerDataSource,
    ): PlayerRepository {
        return PlayerRepositoryImpl(
            playerDataSource = mainPlayerDataSource,
        )
    }

    @Provides
    @Singleton
    @ClipPlayerRepository
    fun provideClipPlayerRepository(
        @ClipPlayerDataSource clipPlayerDataSource: PlayerDataSource,
    ): PlayerRepository {
        return PlayerRepositoryImpl(
            playerDataSource = clipPlayerDataSource,
        )
    }

    @Provides
    @Singleton
    fun provideImageRepository(
        @ApplicationContext context: Context,
    ): ImageRepository {
        return ImageRepositoryImpl(
            context = context,
        )
    }

    @Provides
    @Singleton
    fun provideRecentSearchRepository(
        recentSearchLocalDataSource: RecentSearchLocalDataSource,
    ): RecentSearchRepository {
        return RecentSearchRepositoryImpl(
            recentSearchLocalDataSource = recentSearchLocalDataSource,
        )
    }
}