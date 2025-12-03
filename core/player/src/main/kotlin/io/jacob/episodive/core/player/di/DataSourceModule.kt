package io.jacob.episodive.core.player.di

import androidx.media3.exoplayer.ExoPlayer
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.jacob.episodive.core.common.EpisodivePlayers
import io.jacob.episodive.core.common.Player
import io.jacob.episodive.core.player.datasource.PlayerDataSource
import io.jacob.episodive.core.player.datasource.PlayerDataSourceImpl
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DataSourceModule {
    @Provides
    @Singleton
    @Player(EpisodivePlayers.Main)
    fun provideMainPlayerDataSource(
        @Player(EpisodivePlayers.Main) exoPlayer: ExoPlayer,
    ): PlayerDataSource {
        return PlayerDataSourceImpl(
            player = exoPlayer,
        )
    }

    @Provides
    @Singleton
    @Player(EpisodivePlayers.Clip)
    fun provideClipPlayerDataSource(
        @Player(EpisodivePlayers.Clip) exoPlayer: ExoPlayer,
    ): PlayerDataSource {
        return PlayerDataSourceImpl(
            player = exoPlayer,
        )
    }
}