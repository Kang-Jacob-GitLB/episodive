package io.jacob.episodive.core.player.di

import androidx.media3.exoplayer.ExoPlayer
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.jacob.episodive.core.player.datasource.PlayerDataSource
import io.jacob.episodive.core.player.datasource.PlayerDataSourceImpl
import javax.inject.Qualifier
import javax.inject.Singleton

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class MainPlayerDataSource

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ClipPlayerDataSource

@Module
@InstallIn(SingletonComponent::class)
object DataSourceModule {
    @Provides
    @Singleton
    @MainPlayerDataSource
    fun provideMainPlayerDataSource(
        @MainPlayer exoPlayer: ExoPlayer,
    ): PlayerDataSource {
        return PlayerDataSourceImpl(
            player = exoPlayer,
        )
    }

    @Provides
    @Singleton
    @ClipPlayerDataSource
    fun provideClipPlayerDataSource(
        @ClipPlayer exoPlayer: ExoPlayer,
    ): PlayerDataSource {
        return PlayerDataSourceImpl(
            player = exoPlayer,
        )
    }
}