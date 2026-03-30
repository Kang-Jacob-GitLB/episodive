package io.jacob.episodive.core.data.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.jacob.episodive.core.data.download.EpisodeDownloaderImpl
import io.jacob.episodive.core.domain.download.EpisodeDownloader
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DownloadModule {
    @Provides
    @Singleton
    fun provideEpisodeDownloaderImpl(
        @ApplicationContext context: Context,
    ): EpisodeDownloaderImpl {
        return EpisodeDownloaderImpl(context = context)
    }

    @Provides
    @Singleton
    fun provideEpisodeDownloader(
        impl: EpisodeDownloaderImpl,
    ): EpisodeDownloader {
        return impl
    }
}
