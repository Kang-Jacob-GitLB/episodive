package io.jacob.episodive.core.player.di

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.exoplayer.audio.DefaultAudioSink
import androidx.media3.exoplayer.audio.TeeAudioProcessor
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.jacob.episodive.core.common.EpisodivePlayers
import io.jacob.episodive.core.common.Player
import io.jacob.episodive.core.player.audio.PlaybackSpectrumMonitor
import io.jacob.episodive.core.player.audio.SpeechAudioTap
import io.jacob.episodive.core.player.audio.SpeechPcmSource
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object PlayerModule {
    /**
     * `:core:caption` 은 구체 타입이 아니라 [SpeechPcmSource] 인터페이스만 알아야 한다.
     * 아래 [provideMainExoPlayer] 가 붙이는 것과 **같은 인스턴스**를 돌려줘야 캡처가 실제로
     * Main 플레이어의 오디오에 연결된다 — [SpeechAudioTap] 이 `@Singleton` 이라 Hilt 가
     * 주입하는 인스턴스는 어디서 받든 항상 하나다.
     */
    @Provides
    @Singleton
    fun provideSpeechPcmSource(speechAudioTap: SpeechAudioTap): SpeechPcmSource = speechAudioTap

    /**
     * Main 플레이어에는 라이브 자막(STT)이 먹을 PCM 을 엿듣는 [SpeechAudioTap] 을 붙인다.
     * 전체 에피소드를 듣는 화면이 Main 뿐이라 자막도 여기서만 필요하다.
     */
    @Provides
    @Singleton
    @Player(EpisodivePlayers.Main)
    fun provideMainExoPlayer(
        @ApplicationContext context: Context,
        speechAudioTap: SpeechAudioTap,
    ): ExoPlayer {
        return createExoPlayer(context, speechAudioTap)
    }

    /**
     * 클립 플레이어에만 주파수 분석을 붙인다. 파도 애니메이션이 있는 화면이 클립뿐이라,
     * 전체 재생에까지 오디오 스레드의 일을 늘릴 이유가 없다.
     */
    @Provides
    @Singleton
    @Player(EpisodivePlayers.Clip)
    fun provideClipExoPlayer(
        @ApplicationContext context: Context,
        spectrumMonitor: PlaybackSpectrumMonitor,
    ): ExoPlayer {
        return createExoPlayer(context, spectrumMonitor)
    }

    @OptIn(UnstableApi::class)
    private fun createExoPlayer(
        context: Context,
        audioSink: TeeAudioProcessor.AudioBufferSink? = null,
    ): ExoPlayer {
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
            .apply {
                audioSink?.let { setRenderersFactory(tappedRenderersFactory(context, it)) }
            }
            .build()
    }

    /**
     * PCM 을 엿보는 [TeeAudioProcessor] 를 오디오 체인 앞에 끼운 렌더러 팩토리.
     *
     * `setAudioProcessors` 로 넘긴 프로세서 뒤에 media3 가 `SonicAudioProcessor`(배속)와
     * `SilenceSkippingAudioProcessor` 를 그대로 붙이므로, 배속 재생은 영향받지 않는다.
     * 탭이 Sonic 앞이라 밴드 경계가 원본 샘플레이트 기준으로 유지된다.
     */
    @OptIn(UnstableApi::class)
    private fun tappedRenderersFactory(
        context: Context,
        sink: TeeAudioProcessor.AudioBufferSink,
    ): DefaultRenderersFactory = object : DefaultRenderersFactory(context) {
        override fun buildAudioSink(
            context: Context,
            enableFloatOutput: Boolean,
            enableAudioTrackPlaybackParams: Boolean,
        ): AudioSink = DefaultAudioSink.Builder(context)
            .setEnableFloatOutput(enableFloatOutput)
            .setEnableAudioTrackPlaybackParams(enableAudioTrackPlaybackParams)
            .setAudioProcessors(arrayOf(TeeAudioProcessor(sink)))
            .build()
    }
}
