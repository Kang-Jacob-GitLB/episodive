package io.jacob.episodive.core.network.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.jacob.episodive.core.network.api.ChapterApi
import io.jacob.episodive.core.network.api.EpisodeApi
import io.jacob.episodive.core.network.api.FeedApi
import io.jacob.episodive.core.network.api.PodcastApi
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ApiModule {
    @Provides
    @Singleton
    fun providePodcastApi(retrofit: Retrofit): PodcastApi = retrofit.create(PodcastApi::class.java)

    @Provides
    @Singleton
    fun provideEpisodeApi(retrofit: Retrofit): EpisodeApi = retrofit.create(EpisodeApi::class.java)

    @Provides
    @Singleton
    fun provideFeedApi(retrofit: Retrofit): FeedApi = retrofit.create(FeedApi::class.java)

    @Provides
    @Singleton
    fun provideChapterApi(retrofit: Retrofit): ChapterApi = retrofit.create(ChapterApi::class.java)
}