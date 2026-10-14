package io.jacob.episodive

import android.net.Uri
import androidx.media3.common.AudioAttributes
import androidx.media3.common.DeviceInfo
import androidx.media3.common.FlagSet
import androidx.media3.common.ForwardingPlayer
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Metadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import androidx.media3.common.TrackSelectionParameters
import androidx.media3.common.Tracks
import androidx.media3.common.VideoSize
import androidx.media3.common.text.Cue
import androidx.media3.common.text.CueGroup
import androidx.media3.common.util.UnstableApi
import io.jacob.episodive.core.model.Station

/**
 * 미디어 세션(알림·잠금화면·블루투스)에 넘기는 플레이어. 스테이션이 재생 중이고 지금 방송 중인 프로그램을 알면 세션이 읽는
 * 메타데이터만 프로그램 기준으로 덧씌운다 — 재생 중인 `MediaItem` 은 교체하지 않으므로 재생이 끊기지 않는다.
 *
 * media3 1.8.0 세션이 메타데이터를 읽는 길은 둘이고, 둘 다 여기서 덧씌운다.
 * 1. `Player.getMediaMetadata()` — 세션 초기 상태, 레거시 세션(`MediaSessionCompat`: 잠금화면·블루투스·구형 컨트롤러)이
 *    `onMediaItemTransition`·`onTimelineChanged`·`onMediaMetadataChanged` 때마다 다시 읽는 곳이다
 *    (`MediaSessionLegacyStub.updateMetadataIfChanged`).
 * 2. 플레이어 리스너의 `onMediaMetadataChanged(metadata)` 인자 — 세션이 이 값을 `playerInfo` 에 그대로 넣고, 알림을 그리는
 *    `MediaController` 가 그 `playerInfo` 를 읽는다. 그래서 리스너를 감싸 원본 인자도 덧씌워 넘긴다. 감싸지 않으면
 *    스트림 메타데이터가 올 때마다 원본이 덮어쓴다.
 *
 * 알림 제목은 `title`, 본문은 `artist` 다(`DefaultMediaNotificationProvider`). 큰 아이콘은 `BitmapLoader.loadBitmapFromMetadata`
 * 가 `artworkData` 를 먼저 보고 없을 때만 `artworkUri` 를 읽는다 — 그래서 프로그램 이미지가 있을 때만 `artworkData` 를
 * 비우고 `artworkUri` 를 싣는다. 이미지가 없으면 원래의 이니셜 아트(`artworkData`)를 그대로 둔다.
 *
 * 스테이션이 아니면(에피소드 등) 덧씌울 대상이 없어 원래 플레이어와 똑같이 위임만 한다.
 */
@UnstableApi
internal class OnAirMetadataPlayer(player: Player) : ForwardingPlayer(player) {

    /**
     * @property station 덧씌울 스테이션. 지금 올라 있는 항목의 태그([Station])와 같을 때만 적용한다.
     * @property artist 알림 본문·잠금화면 부제로 쓸 "프로그램 제목 · 진행자".
     * @property artworkUri 프로그램 이미지(https). 없으면 원래 아트를 둔다.
     */
    data class Overlay(
        val station: Station,
        val artist: String,
        val artworkUri: Uri?,
    )

    private var overlay: Overlay? = null

    // 세션이 등록한 원본 리스너. 덧씌움이 바뀔 때 직접 알리려고 따로 쥔다.
    private val listeners = LinkedHashSet<Player.Listener>()

    /** 덧씌움을 바꾸고, 달라졌으면 세션이 알림·잠금화면을 다시 그리도록 메타데이터 변경을 알린다. */
    fun setOverlay(newOverlay: Overlay?) {
        if (newOverlay == overlay) return
        overlay = newOverlay
        val metadata = mediaMetadata
        val events = Player.Events(FlagSet.Builder().add(Player.EVENT_MEDIA_METADATA_CHANGED).build())
        // 순회 중 리스너가 스스로 빠질 수 있어 복사본을 돈다.
        listeners.toList().forEach { listener ->
            listener.onMediaMetadataChanged(metadata)
            listener.onEvents(this, events)
        }
    }

    override fun getMediaMetadata(): MediaMetadata = apply(super.getMediaMetadata())

    override fun addListener(listener: Player.Listener) {
        listeners += listener
        super.addListener(OverlayListener(listener))
    }

    override fun removeListener(listener: Player.Listener) {
        listeners -= listener
        super.removeListener(OverlayListener(listener))
    }

    private fun apply(raw: MediaMetadata): MediaMetadata {
        val o = overlay ?: return raw
        // 올라 있는 항목이 그 스테이션이 아니면(전환 직후·에피소드) 원본 그대로다.
        val playing = currentMediaItem?.localConfiguration?.tag as? Station
        if (playing?.id != o.station.id) return raw

        return raw.buildUpon()
            .setTitle(o.station.name)
            .setDisplayTitle(o.station.name)
            .setArtist(o.artist)
            .apply {
                if (o.artworkUri != null) {
                    // artworkData 가 있으면 BitmapLoader 가 uri 를 무시한다.
                    setArtworkData(null, null)
                    setArtworkUri(o.artworkUri)
                }
            }
            .build()
    }

    /**
     * 원본 리스너를 감싸 `onMediaMetadataChanged` 의 인자만 덧씌운다. 나머지 콜백은 그대로 위임한다. `ForwardingPlayer` 가
     * 리스너 동일성으로 제거하므로 equals/hashCode 는 감싼 리스너를 따른다.
     *
     * `Player.Listener` 의 메서드는 전부 자바 default 라 코틀린의 `by` 위임이 넘겨 주지 않는다(빈 기본 구현이 불린다). 그래서
     * 하나씩 명시한다. media3 를 올려 메서드가 늘면 [OnAirMetadataPlayerTest] 의 전수 검사가 빠진 것을 알려 준다.
     */
    @Suppress("OVERRIDE_DEPRECATION")
    private inner class OverlayListener(
        private val delegate: Player.Listener,
    ) : Player.Listener {

        override fun onMediaMetadataChanged(mediaMetadata: MediaMetadata) {
            delegate.onMediaMetadataChanged(apply(mediaMetadata))
        }

        override fun onEvents(player: Player, events: Player.Events) = delegate.onEvents(player, events)

        override fun onTimelineChanged(timeline: Timeline, reason: Int) = delegate.onTimelineChanged(timeline, reason)

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) =
            delegate.onMediaItemTransition(mediaItem, reason)

        override fun onTracksChanged(tracks: Tracks) = delegate.onTracksChanged(tracks)

        override fun onPlaylistMetadataChanged(mediaMetadata: MediaMetadata) =
            delegate.onPlaylistMetadataChanged(mediaMetadata)

        override fun onIsLoadingChanged(isLoading: Boolean) = delegate.onIsLoadingChanged(isLoading)

        override fun onLoadingChanged(isLoading: Boolean) = delegate.onLoadingChanged(isLoading)

        override fun onAvailableCommandsChanged(availableCommands: Player.Commands) =
            delegate.onAvailableCommandsChanged(availableCommands)

        override fun onTrackSelectionParametersChanged(parameters: TrackSelectionParameters) =
            delegate.onTrackSelectionParametersChanged(parameters)

        override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) =
            delegate.onPlayerStateChanged(playWhenReady, playbackState)

        override fun onPlaybackStateChanged(playbackState: Int) = delegate.onPlaybackStateChanged(playbackState)

        override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) =
            delegate.onPlayWhenReadyChanged(playWhenReady, reason)

        override fun onPlaybackSuppressionReasonChanged(playbackSuppressionReason: Int) =
            delegate.onPlaybackSuppressionReasonChanged(playbackSuppressionReason)

        override fun onIsPlayingChanged(isPlaying: Boolean) = delegate.onIsPlayingChanged(isPlaying)

        override fun onRepeatModeChanged(repeatMode: Int) = delegate.onRepeatModeChanged(repeatMode)

        override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) =
            delegate.onShuffleModeEnabledChanged(shuffleModeEnabled)

        override fun onPlayerError(error: PlaybackException) = delegate.onPlayerError(error)

        override fun onPlayerErrorChanged(error: PlaybackException?) = delegate.onPlayerErrorChanged(error)

        override fun onPositionDiscontinuity(reason: Int) = delegate.onPositionDiscontinuity(reason)

        override fun onPositionDiscontinuity(
            oldPosition: Player.PositionInfo,
            newPosition: Player.PositionInfo,
            reason: Int,
        ) = delegate.onPositionDiscontinuity(oldPosition, newPosition, reason)

        override fun onPlaybackParametersChanged(playbackParameters: PlaybackParameters) =
            delegate.onPlaybackParametersChanged(playbackParameters)

        override fun onSeekBackIncrementChanged(seekBackIncrementMs: Long) =
            delegate.onSeekBackIncrementChanged(seekBackIncrementMs)

        override fun onSeekForwardIncrementChanged(seekForwardIncrementMs: Long) =
            delegate.onSeekForwardIncrementChanged(seekForwardIncrementMs)

        override fun onMaxSeekToPreviousPositionChanged(maxSeekToPreviousPositionMs: Long) =
            delegate.onMaxSeekToPreviousPositionChanged(maxSeekToPreviousPositionMs)

        override fun onAudioSessionIdChanged(audioSessionId: Int) = delegate.onAudioSessionIdChanged(audioSessionId)

        override fun onAudioAttributesChanged(audioAttributes: AudioAttributes) =
            delegate.onAudioAttributesChanged(audioAttributes)

        override fun onVolumeChanged(volume: Float) = delegate.onVolumeChanged(volume)

        override fun onSkipSilenceEnabledChanged(skipSilenceEnabled: Boolean) =
            delegate.onSkipSilenceEnabledChanged(skipSilenceEnabled)

        override fun onDeviceInfoChanged(deviceInfo: DeviceInfo) = delegate.onDeviceInfoChanged(deviceInfo)

        override fun onDeviceVolumeChanged(volume: Int, muted: Boolean) = delegate.onDeviceVolumeChanged(volume, muted)

        override fun onVideoSizeChanged(videoSize: VideoSize) = delegate.onVideoSizeChanged(videoSize)

        override fun onSurfaceSizeChanged(width: Int, height: Int) = delegate.onSurfaceSizeChanged(width, height)

        override fun onRenderedFirstFrame() = delegate.onRenderedFirstFrame()

        override fun onCues(cues: List<Cue>) = delegate.onCues(cues)

        override fun onCues(cueGroup: CueGroup) = delegate.onCues(cueGroup)

        override fun onMetadata(metadata: Metadata) = delegate.onMetadata(metadata)

        override fun equals(other: Any?): Boolean = other is OverlayListener && other.delegate == delegate

        override fun hashCode(): Int = delegate.hashCode()
    }
}
