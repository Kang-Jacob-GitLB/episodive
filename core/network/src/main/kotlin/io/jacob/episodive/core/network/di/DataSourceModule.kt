package io.jacob.episodive.core.network.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.jacob.episodive.core.network.api.ChapterApi
import io.jacob.episodive.core.network.api.EpisodeApi
import io.jacob.episodive.core.network.api.FeedApi
import io.jacob.episodive.core.network.api.PodcastApi
import io.jacob.episodive.core.network.datasource.ChapterRemoteDataSource
import io.jacob.episodive.core.network.datasource.ChapterRemoteDataSourceImpl
import io.jacob.episodive.core.network.datasource.EpisodeRemoteDataSource
import io.jacob.episodive.core.network.datasource.EpisodeRemoteDataSourceImpl
import io.jacob.episodive.core.network.datasource.FeedRemoteDataSource
import io.jacob.episodive.core.network.datasource.FeedRemoteDataSourceImpl
import io.jacob.episodive.core.network.datasource.PodcastRemoteDataSource
import io.jacob.episodive.core.network.datasource.PodcastRemoteDataSourceImpl
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DataSourceModule {
    @Provides
    @Singleton
    fun providePodcastRemoteDataSource(
        podcastApi: PodcastApi,
    ): PodcastRemoteDataSource {
        return PodcastRemoteDataSourceImpl(
            podcastApi = podcastApi,
        )
    }

    @Provides
    @Singleton
    fun provideEpisodeRemoteDataSource(
        episodeApi: EpisodeApi,
    ): EpisodeRemoteDataSource {
        return EpisodeRemoteDataSourceImpl(
            episodeApi = episodeApi,
        )
    }

    @Provides
    @Singleton
    fun provideFeedRemoteDataSource(
        feedApi: FeedApi,
    ): FeedRemoteDataSource {
        return FeedRemoteDataSourceImpl(
            feedApi = feedApi,
        )
    }

    @Provides
    @Singleton
    fun provideChapterRemoteDataSource(
        chapterApi: ChapterApi,
    ): ChapterRemoteDataSource {
        return ChapterRemoteDataSourceImpl(
            chapterApi = chapterApi,
        )
    }
}