package io.jacob.episodive.core.player.di

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.jacob.episodive.core.common.EpisodivePlayers
import io.jacob.episodive.core.common.Player
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object PlayerModule {
    @Provides
    @Singleton
    @Player(EpisodivePlayers.Main)
    fun provideMainExoPlayer(
        @ApplicationContext context: Context
    ): ExoPlayer {
        return createExoPlayer(context)
    }

    @Provides
    @Singleton
    @Player(EpisodivePlayers.Clip)
    fun provideClipExoPlayer(
        @ApplicationContext context: Context
    ): ExoPlayer {
        return createExoPlayer(context)
    }

    @OptIn(UnstableApi::class)
    private fun createExoPlayer(context: Context): ExoPlayer {
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .build()

        val httpDataSourceFactory = DefaultHttpDataSource.Factory()
            .setAllowCrossProtocolRedirects(true) // HTTP -> HTTPS 리다이렉트 허용
            .setUserAgent("Episodive/1.0")

        val dataSourceFactory = DefaultDataSource.Factory(context, httpDataSourceFactory)

        return ExoPlayer.Builder(context)
            .setAudioAttributes(audioAttributes, true)
            .setHandleAudioBecomingNoisy(true)
            .setMediaSourceFactory(DefaultMediaSourceFactory(dataSourceFactory))
            .setSeekBackIncrementMs(15_000L)
            .setSeekForwardIncrementMs(30_000L)
            .build()
    }
}