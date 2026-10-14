package io.jacob.episodive.core.player.datasource

import androidx.annotation.OptIn
import androidx.core.net.toUri
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Metadata
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import androidx.media3.common.Tracks
import androidx.media3.common.text.CueGroup
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.HttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.extractor.metadata.icy.IcyInfo
import androidx.media3.extractor.metadata.id3.TextInformationFrame
import io.jacob.episodive.core.domain.download.EpisodeDownloader
import io.jacob.episodive.core.model.Episode
import io.jacob.episodive.core.model.PlaybackFailure
import io.jacob.episodive.core.model.Progress
import io.jacob.episodive.core.model.Spectrum
import io.jacob.episodive.core.model.Station
import io.jacob.episodive.core.model.StationArt
import io.jacob.episodive.core.model.StationStream
import io.jacob.episodive.core.model.art
import io.jacob.episodive.core.player.audio.PlaybackSpectrumMonitor
import io.jacob.episodive.core.model.coverUrl
import io.jacob.episodive.core.model.mapper.toDurationMillis
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import kotlin.time.Duration

class PlayerDataSourceImpl @Inject constructor(
    private val player: ExoPlayer,
    private val episodeDownloader: EpisodeDownloader,
    // 파도 애니메이션이 있는 클립 플레이어에만 붙는다. 없으면 스펙트럼은 늘 잠잠하다.
    private val spectrumMonitor: PlaybackSpectrumMonitor? = null,
    // 진행 갱신이 도는 범위. 테스트가 가상 시간으로 갱신 코루틴을 결정적으로 돌리려고 바꿔 끼운다.
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
) : PlayerDataSource {

    // prepare() 가 setMediaItems 를 호출하는 동안 transition 콜백의 progress 발행을 막는 플래그.
    // 콜백과 prepare() 는 같은 플레이어 스레드에서만 실행되므로 별도 동기화가 필요 없다.
    private var isPreparing = false

    // 사용자 의도로 현재 항목을 바꾼 호출 수. 복구 쪽이 "그 사이 사용자가 다른 걸 골랐는가" 를
    // 가르는 근거라, 복원(prepare)과 복구(restartStation)는 세지 않는다. 메인 스레드에서만 만진다.
    private var playRequests = 0L

    // 직전 항목이 스테이션이었는지. 배속·반복은 종류가 바뀔 때만 다시 정한다. 에피소드끼리 넘어갈 때
    // 매번 저장값을 되돌려 놓으면 MediaSession(Android Auto 등)으로 플레이어에 직접 건 값이 낡은 저장값으로
    // 덮인다.
    private var wasStation = false

    // 직전에 배속을 제한했는지(스테이션 또는 라이브 에피소드). 반복 모드는 스테이션만 정하므로 따로 둔다.
    private var wasSpeedRestricted = false

    // 현재 창의 라이브 여부를 읽을 때 재사용하는 Window. 플레이어 스레드에서만 쓴다.
    private val liveWindow = Timeline.Window()

    // 이니셜 아트 → 알림 아트 PNG. 같은 아트면 같은 그림이라 스테이션 id 가 아니라 아트로 키를 삼는다.
    // IO 의 선렌더([prepareStationArtwork])와 메인의 폴백 렌더가 함께 만지므로 동시 접근에 안전해야 한다.
    private val stationArtworkCache = ConcurrentHashMap<StationArt, ByteArray>()

    // 지금 스테이션의 소리가 한 번이라도 멎었는지(일시정지·포커스 일시 상실 등). 다시 흐를 때 라이브로
    // 다시 붙을지를 가른다. 다른 스테이션으로 바뀔 때만 푼다 — 멈춘 채 복구(restartStation)가 같은
    // 항목을 다시 올려도 transition 이 나므로, 그때 풀면 재개 시 복구 시점의 버퍼부터 흐른다.
    private var stationStale = false

    private val listener = object : Player.Listener {
        override fun onTimelineChanged(timeline: Timeline, reason: Int) {
            Timber.d(
                "timeline.periodCount=%d, timeline.windowCount=%d, reason: %s"
                    .format(timeline.periodCount, timeline.windowCount, reason)
            )

            when (reason) {
                Player.TIMELINE_CHANGE_REASON_PLAYLIST_CHANGED -> {
                    Timber.d("TIMELINE_CHANGE_REASON_PLAYLIST_CHANGED")
                }

                Player.TIMELINE_CHANGE_REASON_SOURCE_UPDATE -> {
                    Timber.d("TIMELINE_CHANGE_REASON_SOURCE_UPDATE")
                }
            }

            val items = mutableListOf<MediaItem>()
            for (i in 0 until player.mediaItemCount) {
                items.add(player.getMediaItemAt(i))
            }
            _playlist.value = items.mapNotNull { item ->
                item.episodeTag()
            }
            updateLiveState()
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            Timber.d("onMediaItemTransition uri: ${mediaItem?.localConfiguration?.uri}, reason: $reason, mediaId: ${mediaItem?.mediaId}")
            when (reason) {
                Player.MEDIA_ITEM_TRANSITION_REASON_REPEAT -> {
                    Timber.d("MEDIA_ITEM_TRANSITION_REASON_REPEAT")
                }

                Player.MEDIA_ITEM_TRANSITION_REASON_AUTO -> {
                    Timber.d("MEDIA_ITEM_TRANSITION_REASON_AUTO")
                }

                Player.MEDIA_ITEM_TRANSITION_REASON_SEEK -> {
                    Timber.d("MEDIA_ITEM_TRANSITION_REASON_SEEK")
                }

                Player.MEDIA_ITEM_TRANSITION_REASON_PLAYLIST_CHANGED -> {
                    Timber.d("MEDIA_ITEM_TRANSITION_REASON_PLAYLIST_CHANGED")
                }
            }

            val episode = mediaItem?.episodeTag()
            val station = mediaItem?.stationTag()
            _nowPlaying.value = episode
            // 에피소드와 스테이션은 한 항목이 가질 수 있는 둘 중 하나라 같은 자리에서 함께 바꾼다.
            if (station?.id != _nowPlayingStation.value?.id) stationStale = false
            _nowPlayingStation.value = station
            _streamTitle.value = null
            // 스테이션은 반복을 적용하지 않는다. 반복 모드가 켜져 있으면 스트림이 끊겨도 ENDED 가 아니라
            // 같은 항목의 재연결 루프(REPEAT)가 돼 Ended 실패가 나지 않는다. 사용자 값은 _repeat 에
            // 보존해 두었다가 에피소드로 돌아올 때 되돌린다. 라이브 에피소드는 반복을 그대로 둔다 —
            // 끊기면 ENDED 로 드러나는 쪽이 정상이고, 복구 경로가 스테이션 전용이다.
            if ((station != null) != wasStation) {
                wasStation = station != null
                if (station != null) {
                    player.repeatMode = Player.REPEAT_MODE_OFF
                } else {
                    player.repeatMode = _repeat.value
                }
            }
            // 배속은 스테이션뿐 아니라 라이브 에피소드도 막는다. 라이브 여부는 타임라인이 준비돼야 알 수
            // 있어, 새 항목이 라이브면 이 자리가 아니라 뒤따르는 onTimelineChanged 에서 판명된다.
            updateLiveState()
            _indexOfList.value = player.currentMediaItemIndex
            // 재생목록을 통째로 갈아끼우는 중에는 발행하지 않는다. media3 가 setMediaItems 호출
            // 스택 안에서 이 콜백을 인라인 실행하는데, 그때는 아직 목표 항목·위치에 도달하기 전이라
            // 잘못된 쌍이 만들어진다. 그 값이 한 번이라도 발행되면 구독자가 그대로 저장해
            // (세션 스냅샷은 throttle 탓에 정정값을 버린다) 이어듣기 지점이 날아간다.
            // 갈아끼우기가 끝난 뒤 확정값을 한 번만 발행한다.
            if (!isPreparing) {
                // 자동 전환과 반복은 새 항목을 항상 처음부터 재생하므로 0 이 확정이다.
                // 그 경우 player.currentPosition 을 읽으면 아직 이전 항목의 끝 위치일 수 있고,
                // 그 값이 새 에피소드의 위치로 저장되면 듣지도 않은 에피소드가 완료 처리된다.
                // 나머지(탐색·재생목록 교체)는 플레이어가 이미 목표 위치에 가 있다.
                val position = when (reason) {
                    Player.MEDIA_ITEM_TRANSITION_REASON_AUTO,
                    Player.MEDIA_ITEM_TRANSITION_REASON_REPEAT -> Duration.ZERO

                    else -> player.currentPosition.toDurationMillis()
                }
                publishProgress(
                    position = position,
                    buffered = Duration.ZERO,
                    // 에피소드 메타의 duration 을 그대로 쓰지 않는다. 클립은 잘라 올린
                    // 길이만 재생되므로, 전체 길이를 실어 보내면 준비가 끝나 progressUpdater
                    // 가 실제 길이를 읽을 때까지 화면의 남은 시간이 에피소드 전체 길이로
                    // 보였다가 클립 길이로 튄다.
                    //
                    // 길이는 반드시 playbackDuration() 을 거친다. 같은 셈을 여기 펼쳐 쓰면
                    // 그 함수가 바뀌는 날 이 경로만 조용히 옛 규칙을 따른다.
                    // episode 와 같은 mediaItem 에서 나오므로 쌍의 원자성은 지켜진다.
                    duration = mediaItem?.playbackDuration() ?: Duration.ZERO,
                    episodeId = episode?.id,
                )
            }
        }

        override fun onMediaMetadataChanged(mediaMetadata: MediaMetadata) {
            Timber.d(
                "title=%s, artist=%s, albumTitle=%s, albumArtist=%s, displayTitle=%s, subTitle=%s, description=%s, durationMs=%d, artworkData=%s, artworkDatType=%d, artworkUri=%s, trackNumber=%d, totalTrackCount=%d"
                    .format(
                        mediaMetadata.title,
                        mediaMetadata.artist,
                        mediaMetadata.albumTitle,
                        mediaMetadata.albumArtist,
                        mediaMetadata.displayTitle,
                        mediaMetadata.subtitle,
                        mediaMetadata.description,
                        mediaMetadata.durationMs,
                        mediaMetadata.artworkData,
                        mediaMetadata.artworkDataType,
                        mediaMetadata.artworkUri,
                        mediaMetadata.trackNumber,
                        mediaMetadata.totalTrackCount
                    )
            )
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            _playback.value = playbackState
            updateLiveState()
            _isBuffering.value = player.isBufferingToPlay()
            when (playbackState) {
                Player.STATE_IDLE -> Timber.d("STATE_IDLE")
                Player.STATE_BUFFERING -> Timber.d("STATE_BUFFERING")
                Player.STATE_READY -> Timber.d("STATE_READY")
                Player.STATE_ENDED -> {
                    Timber.d("STATE_ENDED")
                    // 라이브는 끝나지 않는다. 스테이션이 ENDED 가 됐다면 서버가 스트림을 끊은 것이다.
                    val item = player.currentMediaItem
                    val station = item?.stationTag()
                    if (station != null) {
                        _failures.tryEmit(
                            PlaybackFailure(
                                cause = PlaybackFailure.Cause.Ended,
                                stationId = station.id,
                                streamUrl = item.localConfiguration?.uri?.toString(),
                            )
                        )
                    }
                }
            }
        }

        override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
            _isBuffering.value = player.isBufferingToPlay()
            rejoinLiveIfStale()
        }

        // 전화 같은 포커스 일시 상실은 playWhenReady 를 끄지 않고 재생만 억제한다. 일시정지와 같이 다룬다.
        override fun onPlaybackSuppressionReasonChanged(playbackSuppressionReason: Int) {
            rejoinLiveIfStale()
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            Timber.d("isPlaying: $isPlaying")
            _isPlaying.value = isPlaying

            // 소리가 멎으면 버퍼가 더 오지 않아 마지막 크기가 그대로 얼어붙는다. 멎는 경로가
            // 여럿(일시정지·정지·오디오 포커스 상실)이라 각 함수가 아니라 이 한 자리에서 푼다.
            if (!isPlaying) spectrumMonitor?.reset()
        }

        override fun onRepeatModeChanged(repeatMode: Int) {
            // 스테이션이 강제로 끈 OFF 는 사용자 값이 아니다. 덮으면 에피소드로 돌아와도 복원할 값이 없다.
            if (currentStation() == null) _repeat.value = repeatMode
            when (repeatMode) {
                Player.REPEAT_MODE_OFF -> Timber.d("REPEAT_MODE_OFF")
                Player.REPEAT_MODE_ONE -> Timber.d("REPEAT_MODE_ONE")
                Player.REPEAT_MODE_ALL -> Timber.d("REPEAT_MODE_ALL")
            }
        }

        override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
            Timber.d("shuffleModeEnabled: $shuffleModeEnabled")
            _isShuffle.value = shuffleModeEnabled
        }

        override fun onPlayerError(error: PlaybackException) {
            Timber.e("errorCode: ${error.errorCode}, errorCodeName: ${error.errorCodeName}, message: ${error.message}, cause: ${error.cause}")
            _failures.tryEmit(error.toFailure(player.currentMediaItem))
        }

        // IcyInfo.rawMetadata·TextInformationFrame 은 @UnstableApi 다. CI 의 lint 가 opt-in 누락을 오류로 잡는다.
        @OptIn(UnstableApi::class)
        override fun onMetadata(metadata: Metadata) {
            if (currentStation() == null) return
            for (i in 0 until metadata.length()) {
                val title = when (val entry = metadata[i]) {
                    is IcyInfo -> decodeIcyTitle(entry.rawMetadata, entry.title)
                    is TextInformationFrame -> entry.takeIf { it.id == "TIT2" }?.values?.firstOrNull()
                    else -> null
                }
                // 제목 정보가 아예 없는 항목은 건드리지 않는다. 빈 제목(곡 사이 멘트·광고)은 지운다 —
                // 남겨 두면 지난 곡이 계속 떠 있다.
                if (title != null) _streamTitle.value = title.trim().ifBlank { null }
            }
        }

        override fun onTracksChanged(tracks: Tracks) {
            val hasTextTrack = tracks.groups.any {
                it.type == C.TRACK_TYPE_TEXT
            }
            Timber.d("hasTextTrack: $hasTextTrack")
        }

        override fun onCues(cueGroup: CueGroup) {
            val currentText = cueGroup.cues.firstOrNull()?.text?.toString()
            _cue.value = currentText ?: ""
        }

        override fun onPositionDiscontinuity(
            oldPosition: Player.PositionInfo,
            newPosition: Player.PositionInfo,
            reason: Int,
        ) {
            // 사용자가 옮긴 위치만 시크로 본다. 자동 전환·무음 건너뛰기 등은 자막을 비울 이유가 없다.
            val isSeek = reason == Player.DISCONTINUITY_REASON_SEEK ||
                    reason == Player.DISCONTINUITY_REASON_SEEK_ADJUSTMENT
            if (isSeek) _seeks.tryEmit(Unit)
        }
    }

    /**
     * 이 에피소드를 올릴 때 걸릴 잘라내기 창. 자르지 않는 경우는
     * [MediaItem.ClippingConfiguration.UNSET] — `MediaItem.Builder` 의 기본값과 같은 값이라
     * 그대로 세워도 "자르지 않음" 이다.
     *
     * [toMediaItem] 과 [playClip] 의 "이미 올라 있는가" 판정이 **둘 다 이 함수를 거친다.**
     * 한쪽이 조건을 손으로 베껴 쓰면 그날로 갈라진다 — 실제로 갈라졌던 적이 있다. [hasClip] 이
     * 거짓인 클립 요청은 창 없이 올라가는데, 판정 쪽만 [Episode.clipEndPositionMs] 와 견주다
     * 보니 UNSET 의 끝값(`C.TIME_END_OF_SOURCE`, 즉 `Long.MIN_VALUE`)과는 영영 같아지지 않아
     * 매번 다시 올렸다. 그 에피소드만 재생 버튼을 누를 때마다 처음으로 되감겼다.
     */
    private fun Episode.clippingConfiguration(isClip: Boolean): MediaItem.ClippingConfiguration =
        if (isClip && hasClip) {
            MediaItem.ClippingConfiguration.Builder()
                .setStartPositionMs(clipStartPositionMs)
                .setEndPositionMs(clipEndPositionMs)
                .build()
        } else {
            MediaItem.ClippingConfiguration.UNSET
        }

    private fun Episode.toMediaItem(isClip: Boolean): MediaItem {
        val mediaMetadata = MediaMetadata.Builder()
            .setTitle(title)
            .setArtist(feedAuthor)
            .setAlbumTitle(feedTitle)
            .setAlbumArtist(feedAuthor)
            .setDisplayTitle(title)
            .setSubtitle(feedTitle)
            .setDescription(description)
            // 빈 문자열을 넘기면 빈 Uri 가 그대로 들어가 잠금화면이 아트를 로드하려다 실패한다.
            .setArtworkUri(coverUrl.ifBlank { null }?.toUri())
            .build()

        // 다운로드 완료 판정은 DB 상태가 아니라 실제 파일 존재로 한다.
        // filePath는 상대경로("feedId/id.ext")이므로 다운로드 디렉토리 기준 절대경로로 변환한다.
        val localPath = filePath
        val uri = if (localPath != null && episodeDownloader.isFileDownloaded(localPath)) {
            android.net.Uri.fromFile(java.io.File(episodeDownloader.getDownloadDirectory(), localPath))
        } else {
            enclosureUrl.toUri()
        }

        val builder = MediaItem.Builder()
            .setMediaId(id.toString())
            .setUri(uri)
            .setTag(this)
            .setMediaMetadata(mediaMetadata)

        builder.setClippingConfiguration(clippingConfiguration(isClip))

        transcriptUrl?.let { url ->
            Timber.i("transcriptUrl:$url")
            builder.setSubtitleConfigurations(
                listOf(
                    MediaItem.SubtitleConfiguration.Builder(url.toUri())
                        .setMimeType(MimeTypes.TEXT_VTT)
                        .setLanguage(feedLanguage)
                        .setSelectionFlags(C.SELECTION_FLAG_DEFAULT)
                        .build()
                )
            )
        }

        return builder.build()
    }

    /**
     * 스테이션을 올릴 아이템. 클리핑·자막은 걸지 않는다.
     *
     * 미디어 id 에 접두어를 붙여 에피소드의 숫자 id 와 겹치지 않게 한다.
     *
     * 아트는 이니셜 PNG 를 바이트로 실어 알림·잠금화면이 바로 쓰게 한다. 비동기로 나중에 갈아끼우면
     * transition 콜백이 다시 돌아 재생 위치 규약이 흔들리므로 **여기서 동기로** 싣는다. 보통은
     * [prepareStationArtwork] 가 IO 에서 캐시를 채워 둬 히트하고, 미스일 때만 메인에서 그린다(폴백).
     */
    private fun Station.toMediaItem(stream: StationStream): MediaItem {
        val mediaMetadata = MediaMetadata.Builder()
            .setTitle(name)
            .setDisplayTitle(name)
            .setArtist(broadcaster)
            .setMediaType(MediaMetadata.MEDIA_TYPE_RADIO_STATION)
            .setIsPlayable(true)
            .setArtworkData(stationArtwork(), MediaMetadata.PICTURE_TYPE_FRONT_COVER)
            .build()

        return MediaItem.Builder()
            .setMediaId("station:$id")
            .setUri(stream.url)
            .setMimeType(if (stream.isHls) MimeTypes.APPLICATION_M3U8 else null)
            .setTag(this)
            .setMediaMetadata(mediaMetadata)
            .build()
    }

    private fun StationArt.cachedArtwork(): ByteArray =
        stationArtworkCache[this] ?: StationArtworkRenderer.render(this).also {
            if (stationArtworkCache.size >= StationArtworkCacheMax) stationArtworkCache.clear()
            stationArtworkCache[this] = it
        }

    private fun Station.stationArtwork(): ByteArray = art.cachedArtwork()

    override suspend fun prepareStationArtwork(station: Station) {
        val art = station.art
        if (stationArtworkCache.containsKey(art)) return
        withContext(Dispatchers.IO) { art.cachedArtwork() }
    }

    /**
     * 멎었던 스테이션이 다시 흐를 때 지금 방송으로 다시 붙는다.
     *
     * 라이브는 멈춘 자리에서 이어 들을 이유가 없다. 그대로 두면 멎기 전에 받아 둔 버퍼부터 흘러 방송보다
     * 멎은 시간만큼 늦게 들리고, HLS 는 그 지점이 창 밖으로 밀려 BehindLiveWindow 가 난다. 재생으로
     * 돌아오는 경로가 여럿(미니바·알림·위젯·MediaSession·포커스 복귀)이라 각 함수가 아니라 플레이어
     * 콜백 한 자리에서 처리한다.
     *
     * 멈추고(소스 해제) → 기본 위치로 옮기고 → 다시 준비한다. 셋 다 필요하다.
     * - 탐색만 하면 안 된다: 탐색이 안 되는 ICY 는 기본 위치가 0 이라, 0 에서 멎었다면 같은 위치로의
     *   탐색을 media3 가 건너뛰어 재연결이 일어나지 않는다. 멈춰서 소스를 놓아야 새 연결로 시작한다.
     * - 멈추고 다시 준비만 해도 안 된다: `stop()` 은 위치를 남겨 두고, 멈춘 동안에도 HLS 재생목록은
     *   갱신되므로 다시 준비하면 정지했던 그 방송 시점으로 돌아간다. IDLE 에서의 `seekToDefaultPosition`
     *   이 남은 위치를 지워야 새 타임라인의 기본 위치(라이브 끝)에서 시작한다.
     * 항목을 바꾸지 않으므로 transition 은 나지 않는다.
     */
    private fun rejoinLiveIfStale() {
        if (currentStation() == null) return
        val flowing = player.playWhenReady &&
            player.playbackSuppressionReason == Player.PLAYBACK_SUPPRESSION_REASON_NONE
        if (!flowing) {
            stationStale = true
        } else if (stationStale) {
            stationStale = false
            player.stop()
            player.seekToDefaultPosition()
            player.prepare()
        }
    }

    /**
     * 이 미디어 아이템을 재생할 때 실제로 흐르는 길이.
     *
     * 준비가 끝나기 전에는 `player.duration` 이 TIME_UNSET 이라 메타에서 길이를 가져와야
     * 하는데, 그때 어느 쪽을 가져올지는 이 함수가 정한다.
     *
     * **메타에서 길이를 가져오는 지점은 예외 없이 이 함수를 거칠 것.** 한 곳이라도 빠뜨리면
     * 그 경로에서만 준비 전후로 값이 달라져 화면의 시간이 튄다.
     *
     * 잘라 올린 아이템은 에피소드에 되묻지 않고 **클리핑 창에서 직접 잰다.** 그 창이 곧
     * media3 가 흘릴 구간이라 어긋날 여지가 없다 — [toMediaItem] 의 조건을 바꾸더라도
     * 여기서 나오는 값은 늘 실제로 올라간 것을 따른다. (화면은 아직 올리기 전이라 되물을
     * 아이템이 없어 [Episode.clipPlaybackDuration] 을 쓴다. 그쪽만 [Episode.hasClip] 기준이다.)
     */
    private fun MediaItem.playbackDuration(): Duration {
        // 클리핑 창을 먼저 잰다. 이 폭은 태그가 없어도 알 수 있는 확정값이라, 태그부터
        // 확인하고 없다고 0 을 돌려주면 알 수 있는 길이를 스스로 버리는 셈이 된다.
        //
        // 밀리초 쪽을 읽는다. 마이크로초 변형이 더 정밀해 보이지만 @UnstableApi 라 lint 가
        // 막는다.
        //
        // 끝을 지정하지 않은 클리핑은 센티널을 직접 견줘 걸러낸다. 뺄셈 결과가 음수라
        // isPositive 에 걸릴 것 같지만, TIME_END_OF_SOURCE 가 Long 최솟값 근처라 시작이
        // 0 보다 크면 뺄셈이 넘쳐 거대한 양수가 된다 — 억 단위 연 수에 해당하는 길이가
        // 발행된다(정확한 자릿수는 래핑 결과에 달렸다).
        if (isClipped()) {
            val clip = clippingConfiguration
            if (clip.endPositionMs != C.TIME_END_OF_SOURCE) {
                // 끝이 정해져 있으면 그 폭이 전부다. 폭이 0 이하로 나와도 에피소드 길이로
                // 물러서지 않는다 — 흐를 것이 없다는 뜻이지 "끝까지" 라는 뜻이 아니다.
                val window = (clip.endPositionMs - clip.startPositionMs).toDurationMillis()
                return window.coerceAtLeast(Duration.ZERO)
            }
            // 끝을 지정하지 않은 클리핑("여기서부터 끝까지")이면 시작 뒤로 남은 만큼만 흐른다.
            // 에피소드 전체 길이를 그대로 돌려주면 시작 오프셋만큼 부풀린 값이 된다.
            val remainder = (episodeTag()?.duration ?: Duration.ZERO) -
                    clip.startPositionMs.toDurationMillis()
            return remainder.coerceAtLeast(Duration.ZERO)
        }
        return episodeTag()?.duration ?: Duration.ZERO
    }

    /**
     * 이 아이템에 실린 에피소드. [toMediaItem] 이 `setTag` 로 붙여 둔 것이다.
     *
     * 길이와 id 를 각각 꺼내며 캐스트를 되풀이하지 않도록 한 곳으로 모은다 — 따로 꺼내면
     * 태그가 에피소드가 아닐 때 한쪽은 0, 다른 쪽은 null 로 갈라진 답을 내놓는다.
     */
    private fun MediaItem.episodeTag(): Episode? = localConfiguration?.tag as? Episode

    /** 이 아이템에 실린 스테이션. [Station.toMediaItem] 이 `setTag` 로 붙여 둔 것이다. */
    private fun MediaItem.stationTag(): Station? = localConfiguration?.tag as? Station

    /**
     * 잘라 올린 아이템인지 — 즉 [toMediaItem] 이 `setClippingConfiguration` 을 불렀는지.
     *
     * 시작·끝 위치를 각각 들여다보지 않고 [MediaItem.ClippingConfiguration.UNSET] 과
     * 통째로 견준다. 묻고 싶은 것("클리핑을 걸었는가")을 그대로 옮긴 형태라, 어느 필드를
     * 어떤 센티널과 견줘야 하는지 알 필요가 없다.
     */
    private fun MediaItem.isClipped(): Boolean =
        clippingConfiguration != MediaItem.ClippingConfiguration.UNSET

    /**
     * 재생을 요청했는데 아직 소리를 낼 만큼 받지 못한 상태. 멈춘 채 시크해도 BUFFERING 이
     * 되므로 playbackState 만 보면 멈춘 플레이어에 스피너가 돈다 — playWhenReady 를 함께 본다.
     */
    private fun Player.isBufferingToPlay(): Boolean =
        playWhenReady && playbackState == Player.STATE_BUFFERING

    override fun getPlayer(): Player {
        return player
    }

    // Intentionally only calls prepare() without play() — restores the player in a paused state
    // so the user can choose when to start playback after app restart.
    override fun prepare(episodes: List<Episode>, indexToPlay: Int, positionMs: Long) {
        val mediaItems = episodes.map { it.toMediaItem(isClip = false) }
        // media3 는 setMediaItems 안에서 onMediaItemTransition 을 인라인 실행한다. 그 사이의
        // 발행을 막아, 복원 위치가 잘못된 중간값으로 한 번도 노출되지 않게 한다.
        isPreparing = true
        try {
            // stop() 은 playWhenReady 를 건드리지 않는다. 스테이션을 듣다 멈춘 뒤 복원하면 true 가 남아 있어
            // 복원이 곧바로 재생으로 이어진다. 복원은 언제나 일시정지로 올린다. prepare 앞에서 꺼야 한다 —
            // prepare 가 남은 true 로 오디오 포커스를 요청하면 뒤늦게 꺼도 반납되지 않는다.
            player.playWhenReady = false
            player.setMediaItems(mediaItems, indexToPlay, positionMs)
            player.prepare()
        } finally {
            isPreparing = false
        }

        // 확정값을 여기서 한 번만 발행한다.
        // 이 시점 player.duration 은 아직 TIME_UNSET 이므로 미디어 아이템의 메타 길이를 쓴다.
        // 지금은 이 경로가 늘 isClip = false 로 올리지만, 그래도 playbackDuration() 을 거친다 —
        // 여기만 예외로 두면 나중에 이 경로로 클립을 복원할 때 조용히 전체 길이를 싣게 된다.
        publishStartOfPlayback(
            mediaItem = mediaItems.getOrNull(indexToPlay),
            position = positionMs.toDurationMillis(),
        )
    }

    override fun play(episode: Episode) {
        Timber.i("url: ${episode.enclosureUrl}")
        playRequests++
        val mediaItem = episode.toMediaItem(isClip = false)

        player.setMediaItem(mediaItem)
        player.prepare()
        player.playWhenReady = true
    }

    override fun play(episodes: List<Episode>, indexToPlay: Int?) {
        episodes.forEachIndexed { index, episode ->
            Timber.i("[$index] url: ${episode.enclosureUrl}")
        }
        playRequests++
        val mediaItems = episodes.map { it.toMediaItem(isClip = false) }

        // setMediaItems 는 목록의 첫 항목으로 transition 을 인라인 발생시킨다. 목표가 첫 항목이
        // 아니면 그 사이에 "재생하지도 않을 에피소드 + 위치 0" 이 발행되어 그 에피소드의
        // 이어듣기 지점이 지워진다. seekToDefaultPosition 으로 목표에 도달할 때까지 막는다.
        isPreparing = true
        try {
            player.setMediaItems(mediaItems)
            indexToPlay?.let { player.seekToDefaultPosition(it) }
            player.prepare()
        } finally {
            isPreparing = false
        }
        player.playWhenReady = true

        publishStartOfPlayback(mediaItems.getOrNull(indexToPlay ?: 0), position = Duration.ZERO)
    }

    /**
     * 클립 하나를 올려 재생한다. **이미 올라가 흐르고 있는 클립이면 아무것도 하지 않는다.**
     *
     * `setMediaItem` 은 듣던 지점을 지운다. "같은 것을 다시 틀어 달라" 는 요청은 대개 화면이
     * 무엇이 올라가 있는지 잘못 알고 보낸 것이라, 그대로 따르면 사용자가 듣던 자리가 사라진다.
     * 화면이 기억으로 막으려 하면 그 기억이 컴포지션과 함께 사라지거나 실제와 어긋나므로,
     * 실제로 무엇이 올라가 있는지 아는 이 자리에서 막는다.
     *
     * 끝난 뒤(ENDED)와 비어 있을 때(IDLE)는 통과시킨다 — 그때 같은 클립을 다시 트는 것은
     * "처음부터 다시 듣기" 라는 정상 요청이고, 지울 지점도 없다.
     */
    override fun playClip(episode: Episode) {
        // 같은 에피소드라도 잘라낸 창이 다르면 다시 올려야 한다. soundbites 는 episodeId 가
        // 기본키라, 캐시가 갱신되면 같은 에피소드의 창만 바뀐 행이 온다. 그때 그냥 넘기면
        // 플레이어는 옛 창을 흘리는데 카드는 새 길이를 보여준다.
        //
        // 견줄 창은 반드시 clippingConfiguration() 에서 받는다 — toMediaItem 이 실제로 세우는
        // 바로 그 값이라, 자르지 않고 올라가는 경우(hasClip 이 거짓)까지 함께 맞는다.
        // 태그부터 견준다. 우리가 올린 아이템일 때만 클리핑 창을 들여다본다.
        val loaded = player.currentMediaItem
        val alreadyLoaded = loaded != null &&
                loaded.episodeTag()?.id == episode.id &&
                loaded.clippingConfiguration == episode.clippingConfiguration(isClip = true) &&
                player.playbackState != Player.STATE_IDLE &&
                player.playbackState != Player.STATE_ENDED
        if (alreadyLoaded) {
            // 다시 올리지는 않되, 멈춰 있으면 이어서 튼다. 아무것도 하지 않고 돌아가면
            // 멈춰 둔 클립의 재생 버튼이 죽는다 — 눌러도 소리가 나지 않는다.
            if (!player.isPlaying) player.play()
            Timber.i("playClip resumed in place: ${episode.id}")
            return
        }

        Timber.i("url: ${episode.enclosureUrl}, clipStartTime: ${episode.clipStartTime}, clipDuration: ${episode.clipDuration}")
        playRequests++
        val mediaItem = episode.toMediaItem(isClip = true)

        player.setMediaItem(mediaItem)
        player.prepare()
        player.playWhenReady = true
    }

    override fun playClips(episodes: List<Episode>, indexToPlay: Int?) {
        episodes.forEachIndexed { index, episode ->
            Timber.i("[$index] url: ${episode.enclosureUrl}, clipStartTime: ${episode.clipStartTime}, clipDuration: ${episode.clipDuration}")
        }
        playRequests++
        val mediaItems = episodes.map { it.toMediaItem(isClip = true) }

        // play(episodes, indexToPlay) 와 같은 이유로 목표 항목에 도달할 때까지 발행을 막는다.
        isPreparing = true
        try {
            player.setMediaItems(mediaItems)
            indexToPlay?.let { player.seekToDefaultPosition(it) }
            player.prepare()
        } finally {
            isPreparing = false
        }
        player.playWhenReady = true

        publishStartOfPlayback(mediaItems.getOrNull(indexToPlay ?: 0), position = Duration.ZERO)
    }

    override fun playIndex(index: Int) {
        playRequests++
        if (index in 0 until player.mediaItemCount) {
            player.seekToDefaultPosition(index)
            player.playWhenReady = true
        }
    }

    override fun playOrPause() {
        if (player.isPlaying) {
            player.pause()
        } else {
            player.play()
        }
    }

    override fun pause() {
        if (player.isPlaying) {
            player.pause()
        }
    }

    override fun resume() {
        if (!player.isPlaying) {
            player.play()
        }
    }

    override fun stop() {
        playRequests++
        clearPlayer()
    }

    /**
     * 멈추고 항목을 비운다. 사용자 정지([stop])와 복구의 정지([stopStation])가 함께 쓴다 — 사용자 호출 수는
     * 호출자가 정한다.
     *
     * 진행 상태를 여기서 따로 비우지 않는다. clearMediaItems() 가 목록을 비우면 media3 가
     * onMediaItemTransition(null, PLAYLIST_CHANGED) 를 부르고, 그 경로가 이미 비어 있는
     * 진행 상태(episodeId = null)를 발행한다. 같은 값을 여기서 한 번 더 실어 보내면
     * "이 자리가 그 일을 한다" 는 인상만 남고 실제로 지키는 것은 없다.
     */
    private fun clearPlayer() {
        player.stop()
        player.clearMediaItems()
    }

    override fun playStation(station: Station, stream: StationStream) {
        Timber.i("station: ${station.id}, url: ${stream.url}, isHls: ${stream.isHls}")
        playRequests++
        // 단일 항목이라 isPreparing 이 필요 없다. 발행은 transition 콜백이 한다.
        player.setMediaItem(station.toMediaItem(stream))
        player.prepare()
        player.playWhenReady = true
        // 막 새로 연결했으니 멎은 적이 없다. 같은 스테이션을 멈춰 둔 채 다시 고르면 플래그가 남아 있어
        // 위의 playWhenReady 콜백이 한 번 더 연결한다. 다만 통화처럼 재생이 억제된 채 골랐다면 그동안
        // 받은 버퍼가 늦으므로 억제가 풀릴 때 다시 붙도록 남긴다.
        stationStale = player.playbackSuppressionReason != Player.PLAYBACK_SUPPRESSION_REASON_NONE
    }

    override fun restartStation(stationId: String, stream: StationStream?): Boolean {
        val current = player.currentMediaItem
        val station = current?.stationTag()?.takeIf { it.id == stationId } ?: return false
        // 복구는 사용자 의도가 아니므로 playRequests 를 세지 않고, 재생/일시정지 상태도 건드리지 않는다.
        player.setMediaItem(stream?.let { station.toMediaItem(it) } ?: current)
        player.prepare()
        return true
    }

    override fun stopStation(stationId: String): Boolean {
        if (currentStation()?.id != stationId) return false
        // 복구의 정지는 사용자 호출이 아니므로 playRequests 를 올리지 않는다.
        clearPlayer()
        return true
    }

    override fun playRequestCount(): Long = playRequests

    override fun next() {
        playRequests++
        if (player.hasNextMediaItem()) {
            player.seekToNextMediaItem()
            player.playWhenReady = true
        }
    }

    override fun previous() {
        playRequests++
        if (player.hasPreviousMediaItem()) {
            player.seekToPrevious()
            player.playWhenReady = true
        }
    }

    override fun seekTo(position: Long) {
        player.seekTo(position)
        // 준비 전에는 player.duration 이 TIME_UNSET(음수)이라 그대로 실으면
        // 시커와 수면 타이머가 쓰레기 값을 본다. 그럴 땐 에피소드 메타의 길이로 대신하되,
        // 잘라 올린 아이템이면 클립 길이를 쓴다(전체 길이를 쓰면 준비 전후로 값이 튄다).
        // 직전 progress 의 duration 을 쓰면 안 된다 — 전환 직후엔 그것이 이전 에피소드의
        // 길이여서, 짧은 길이 + 큰 위치 조합이 완료 판정을 잘못 뒤집는다.
        //
        // 아이템은 한 번만 읽는다. 길이와 id 를 따로 읽으면 그 사이에 전환이 끼어들어
        // "A 의 길이 + B 의 id" 가 나갈 수 있고, 그 쌍이 원자적이라는 것이 publishProgress
        // 의 계약이다. 수면 타이머가 바로 이 쌍으로 남은 시간을 셈한다.
        val item = player.currentMediaItem
        val duration = player.duration.toDurationMillis().takeIf { it.isPositive() }
            ?: item?.playbackDuration()
            ?: Duration.ZERO
        publishProgress(
            position = position.toDurationMillis(),
            buffered = player.bufferedPosition.toDurationMillis(),
            duration = duration,
            episodeId = item?.episodeTag()?.id,
        )
    }

    override fun seekBackward() {
        player.seekBack()
    }

    override fun seekForward() {
        player.seekForward()
    }

    override fun setShuffle(isShuffle: Boolean) {
        player.shuffleModeEnabled = isShuffle
    }

    override fun shuffle() {
        val newState = !player.shuffleModeEnabled
        player.shuffleModeEnabled = newState
    }

    override fun setRepeat(repeat: Int) {
        // 스테이션 중에는 값만 저장한다(setSpeed 와 같은 이유). 에피소드로 돌아오면 transition 이 적용한다.
        if (currentStation() != null) _repeat.value = repeat else player.repeatMode = repeat
    }

    override fun changeRepeat() {
        val isStation = currentStation() != null
        val newState = when (if (isStation) _repeat.value else player.repeatMode) {
            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ONE
            Player.REPEAT_MODE_ONE -> Player.REPEAT_MODE_ALL
            Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_OFF
            else -> return
        }
        if (isStation) _repeat.value = newState else player.repeatMode = newState
    }

    override fun setSpeed(speed: Float) {
        val safeSpeed = speed.coerceIn(0.5f, 3.5f)
        _speed.value = safeSpeed
        // 스테이션·라이브 에피소드 중에는 값만 저장한다. PlayerViewModel 이 생성될 때마다 사용자 배속을 setSpeed 로
        // 보내므로 여기서 막지 않으면 라이브 스트림이 배속으로 돌아 버퍼가 마른다. 에피소드로
        // 돌아오면 [updateLiveState] 가 저장해 둔 값을 다시 적용한다.
        if (!isSpeedRestricted()) player.setPlaybackSpeed(safeSpeed)
    }

    override fun setVolume(volume: Float) {
        player.volume = volume.coerceIn(0f, 1f)
    }

    override fun addTrack(episode: Episode, index: Int?) {
        val mediaItem = episode.toMediaItem(isClip = false)

        index?.let {
            player.addMediaItem(it, mediaItem)
        } ?: run {
            player.addMediaItem(mediaItem)
        }
    }

    override fun addTrack(episodes: List<Episode>, index: Int?) {
        val mediaItems = episodes.map { it.toMediaItem(isClip = false) }

        index?.let {
            player.addMediaItems(it, mediaItems)
        } ?: run {
            player.addMediaItems(mediaItems)
        }
    }

    override fun addClipTrack(episode: Episode, index: Int?) {
        val mediaItem = episode.toMediaItem(isClip = true)

        index?.let {
            player.addMediaItem(it, mediaItem)
        } ?: run {
            player.addMediaItem(mediaItem)
        }
    }

    override fun addClipTracks(episodes: List<Episode>, index: Int?) {
        val mediaItems = episodes.map { it.toMediaItem(isClip = true) }

        index?.let {
            player.addMediaItems(it, mediaItems)
        } ?: run {
            player.addMediaItems(mediaItems)
        }
    }

    override fun removeTrack(index: Int) {
        player.removeMediaItem(index)
    }

    override fun clearPlayList() {
        player.clearMediaItems()
    }

    override fun release() {
        scope.cancel()
        player.release()
        player.removeListener(listener)
    }

    override fun rehydrate(episode: Episode) {
        // 플레이어가 이미 다른 항목을 들고 있으면 그쪽이 진실이다.
        // 이 가드가 없으면 서비스의 비동기 DB 읽기가 늦게 도착해 _nowPlaying 을 되돌리고,
        // UI 는 A 를 보여주면서 B 를 재생하는 상태가 된다.
        // 태그가 있으면 "다른 에피소드" 가 아니라 "다른 항목" 으로 가른다. 에피소드가 아닌 항목
        // (라이브 스트림)은 currentEpisode() 가 null 이라, 에피소드끼리만 견주면 그대로 통과해 덮어쓴다.
        // 태그가 아예 없는 항목은 예전처럼 통과시킨다. 외부 컨트롤러가 IPC 로 넘긴 항목은 태그를 잃고
        // 오므로, 그것까지 막으면 rehydrate 가 메우려던 빈 nowPlaying 이 그대로 남는다.
        val currentTag = player.currentMediaItem?.localConfiguration?.tag
        if (currentTag != null && (currentTag as? Episode)?.id != episode.id) return
        // 같은 episode 면 무시 (불필요한 widget 갱신/depounce 방지).
        if (_nowPlaying.value?.id == episode.id) return
        _nowPlaying.value = episode
        _isPlaying.value = player.isPlaying
        // 여기서 _progress 를 세팅하지 말 것. 이 시점의 실제 재생 위치는 알 수 없고,
        // episodeId 가 붙은 progress 를 발행하는 순간 그 값이 DB 에 저장되어
        // 아직 듣지도 않은 위치로 이어듣기 지점이 덮어써진다.
        // progress 는 progressUpdater 나 transition 콜백이 실제 값으로 채운다.
    }

    override fun hasMediaItem(): Boolean = player.currentMediaItem != null

    /**
     * 지금 플레이어에 올라 있는 에피소드. 재생 위치의 주인을 판별하는 유일한 근거다.
     *
     * `_nowPlaying` 을 쓰지 않는 이유: rehydrate 가 플레이어 상태와 무관하게 그 값을 바꿀 수 있어,
     * 실제로는 B 를 재생하면서 A 의 id 가 붙은 위치를 발행하는 오염이 생긴다.
     * 플레이어 스레드에서만 호출할 것.
     */
    private fun currentEpisode(): Episode? =
        player.currentMediaItem?.episodeTag()

    private fun currentEpisodeId(): Long? = currentEpisode()?.id

    /**
     * 지금 항목이 라이브 에피소드인가. 피드 메타로 점치지 않고 플레이어가 본 창으로 가른다 — 피드의
     * 라이브 항목엔 길이·상태 같은 일관된 표지가 없다. 준비 전 placeholder 창은 아직 모른다.
     * 스테이션은 에피소드가 아니라 여기서 제외한다.
     */
    private fun isLiveEpisode(): Boolean {
        if (currentEpisode() == null) return false
        val timeline = player.currentTimeline
        if (timeline.isEmpty) return false
        val window = timeline.getWindow(player.currentMediaItemIndex, liveWindow)
        return !window.isPlaceholder && window.isLive()
    }

    private fun isSpeedRestricted(): Boolean =
        currentStation() != null || _liveEpisodeId.value != null

    /**
     * 라이브 에피소드 판정을 갱신하고, 바뀌면 배속 제한을 맞춘다. 라이브는 1x 가 아니면 버퍼가 마른다.
     * 사용자 값은 [_speed] 에 보존해 두었다가 제한이 풀릴 때 되돌린다. 종류가 바뀔 때만 건드린다 —
     * 매번 되돌리면 MediaSession 으로 플레이어에 직접 건 값이 낡은 저장값으로 덮인다.
     */
    private fun updateLiveState() {
        _liveEpisodeId.value = if (isLiveEpisode()) currentEpisodeId() else null
        val restricted = isSpeedRestricted()
        if (restricted == wasSpeedRestricted) return
        wasSpeedRestricted = restricted
        player.setPlaybackSpeed(if (restricted) 1f else _speed.value)
    }

    /** 지금 플레이어에 올라 있는 스테이션. [currentEpisode] 와 같은 이유로 플레이어에서 읽는다. */
    private fun currentStation(): Station? =
        player.currentMediaItem?.stationTag()

    /** 플레이어 오류를 복구가 갈라 볼 수 있는 원인으로 분류한다. */
    @OptIn(UnstableApi::class)
    private fun PlaybackException.toFailure(item: MediaItem?): PlaybackFailure {
        val kind = when (errorCode) {
            PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS -> {
                when ((cause as? HttpDataSource.InvalidResponseCodeException)?.responseCode) {
                    401, 403, 410 -> PlaybackFailure.Cause.Expired
                    in 500..599 -> PlaybackFailure.Cause.Network
                    else -> PlaybackFailure.Cause.Gone
                }
            }

            PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED,
            PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT,
            PlaybackException.ERROR_CODE_IO_UNSPECIFIED,
            PlaybackException.ERROR_CODE_TIMEOUT -> PlaybackFailure.Cause.Network

            PlaybackException.ERROR_CODE_BEHIND_LIVE_WINDOW -> PlaybackFailure.Cause.BehindLiveWindow

            PlaybackException.ERROR_CODE_PARSING_CONTAINER_MALFORMED,
            PlaybackException.ERROR_CODE_PARSING_MANIFEST_MALFORMED,
            PlaybackException.ERROR_CODE_PARSING_CONTAINER_UNSUPPORTED,
            PlaybackException.ERROR_CODE_PARSING_MANIFEST_UNSUPPORTED,
            PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND,
            PlaybackException.ERROR_CODE_IO_INVALID_HTTP_CONTENT_TYPE,
            PlaybackException.ERROR_CODE_IO_CLEARTEXT_NOT_PERMITTED,
            PlaybackException.ERROR_CODE_DECODING_FORMAT_UNSUPPORTED -> PlaybackFailure.Cause.Gone

            else -> PlaybackFailure.Cause.Other
        }
        return PlaybackFailure(
            cause = kind,
            stationId = item?.stationTag()?.id,
            streamUrl = item?.localConfiguration?.uri?.toString(),
        )
    }

    /**
     * 재생목록을 갈아끼운 직후, 실제로 재생할 항목의 상태를 발행한다.
     * 갈아끼우는 동안 억제한 transition 콜백을 대신하는 확정 발행이다.
     *
     * 에피소드가 아니라 [MediaItem] 을 받는다. 클립인지 아닌지를 호출자가 넘기게 하면 판정이
     * 두 곳으로 갈라져 [playbackDuration] 의 기준과 어긋날 수 있다 — 실제로 잘라 올린 그
     * 아이템에게 직접 묻는 편이 어긋날 여지가 없다.
     *
     * @param position 발행할 재생 위치. 기본값을 두지 않는다 — 목록을 새로 트는
     * 경로(`play`·`playClips`)는 0 이지만 `prepare` 는 듣던 자리를 복원하므로, 잊고 빠뜨리면
     * 이어듣기 지점이 0 으로 발행되어 그대로 저장된다. 컴파일이 막게 두는 편이 낫다.
     */
    private fun publishStartOfPlayback(mediaItem: MediaItem?, position: Duration) {
        publishProgress(
            position = position,
            buffered = Duration.ZERO,
            duration = mediaItem?.playbackDuration() ?: Duration.ZERO,
            episodeId = mediaItem?.episodeTag()?.id,
        )
    }

    /**
     * progress 를 발행하는 통로. 위치·길이와 그것이 속한 episodeId 를 한 번의 대입으로 묶는다.
     *
     * 구독자(재생 위치 저장 경로)는 이 쌍을 원자적으로 받아야 한다. 에피소드 id 를 별도 Flow 에서
     * 가져오면 전환 순간 두 스트림의 지연 차 때문에 어긋난 쌍을 보게 되고, 이전 에피소드의
     * 저장 위치가 오염된다. 새 발행 지점을 추가할 때도 반드시 이 함수를 거칠 것.
     *
     * 단일 항목 재생(`play(episode)`, `playClip`)과 `addTrack` 계열은 여기를 직접 거치지 않는다.
     * 그 경로들은 transition 콜백이 대신 발행하며, 콜백도 이 함수를 거치므로 계약은 지켜진다.
     * 목록을 통째로 갈아끼우는 경로만 콜백을 억제하고 확정값을 직접 발행한다.
     */
    private fun publishProgress(
        position: Duration,
        buffered: Duration,
        duration: Duration,
        episodeId: Long?,
    ) {
        _progress.value = Progress(
            position = position,
            buffered = buffered,
            duration = duration,
            episodeId = episodeId,
        )
    }

    private val _nowPlaying = MutableStateFlow<Episode?>(null)
    override val nowPlaying: Flow<Episode?> = _nowPlaying

    private val _nowPlayingStation = MutableStateFlow<Station?>(null)
    override val nowPlayingStation: Flow<Station?> = _nowPlayingStation

    private val _liveEpisodeId = MutableStateFlow<Long?>(null)
    override val liveEpisodeId: Flow<Long?> = _liveEpisodeId

    private val _streamTitle = MutableStateFlow<String?>(null)
    override val streamTitle: Flow<String?> = _streamTitle

    // replay 를 두지 않는다. 같은 스테이션을 다시 틀 때 지난 실패가 재생되면 멀쩡한 세션에 복구가 돈다.
    // 구독자가 없을 때의 실패는 버려지는 것이 맞다 — 복구는 지금 듣고 있는 쪽의 일이다.
    private val _failures = MutableSharedFlow<PlaybackFailure>(
        extraBufferCapacity = 8,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    override val failures: Flow<PlaybackFailure> = _failures

    private val _playlist = MutableStateFlow<List<Episode>>(emptyList())
    override val playlist: Flow<List<Episode>> = _playlist

    private val _indexOfList = MutableStateFlow(0)
    override val indexOfList: Flow<Int> = _indexOfList

    private val _playback = MutableStateFlow(Player.STATE_IDLE)
    override val playback: Flow<Int> = _playback

    private val _isPlaying = MutableStateFlow(false)
    override val isPlaying: Flow<Boolean> = _isPlaying

    private val _isBuffering = MutableStateFlow(false)
    override val isBuffering: Flow<Boolean> = _isBuffering

    private val _isShuffle = MutableStateFlow(false)
    override val isShuffle: Flow<Boolean> = _isShuffle

    private val _repeat = MutableStateFlow(Player.REPEAT_MODE_OFF)
    override val repeat: Flow<Int> = _repeat

    private val _speed = MutableStateFlow(1.0f)
    override val speed: Flow<Float> = _speed

    private val _cue = MutableStateFlow("")
    override val cue: Flow<String> = _cue

    // 시크는 "일어났다" 는 사실만 있으면 되는 일회성 신호라 StateFlow 로 두지 않는다 — 컨플레이션이
    // 일어나도 무방하도록(구독이 느려 놓쳐도 다음 tick 이 정상 상태이므로) extraBufferCapacity 로
    // 여유를 두고 DROP_OLDEST 로 발행 스레드(플레이어 콜백)를 절대 suspend 시키지 않는다.
    private val _seeks = MutableSharedFlow<Unit>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    override val seeks: Flow<Unit> = _seeks

    override val spectrum: Flow<Spectrum> = spectrumMonitor?.spectrum ?: flowOf(Spectrum.Silent)

    private val _progress = MutableStateFlow(
        Progress(
            position = Duration.ZERO,
            buffered = Duration.ZERO,
            duration = Duration.ZERO,
        )
    )
    override val progress: Flow<Progress> = _progress

    private val progressUpdater: Flow<Unit> = combine(
        _isPlaying,
        _playback
    ) { isPlaying, playback ->
        isPlaying to playback
    }.flatMapLatest { (isPlaying, playback) ->
        flow {
            while (isPlaying) {
                // 위치와 episodeId 를 반드시 같은 Main 블록 안에서 **읽는다**. transition 콜백도
                // 같은 스레드에서 실행되므로, 이렇게 해야 둘이 서로 맞는 쌍이 된다.
                // 블록 밖에서 읽으면 전환 도중의 어긋난 쌍이 만들어질 수 있다.
                //
                // 발행은 블록 밖(IO)에서 일어나므로 "읽기~발행" 사이에 전환이 끼어들 수 있다.
                // 그래도 발행되는 쌍 자체는 일관되므로(이전 에피소드 + 그 에피소드의 실제 위치)
                // 저장이 오염되지는 않는다. 잠깐 오래된 쌍이 보일 뿐이고 다음 tick 이 정정한다.
                val progressValue = withContext(Dispatchers.Main) {
                    val duration = player.duration.toDurationMillis()
                    if (duration.isPositive()) {
                        Progress(
                            position = player.currentPosition.toDurationMillis(),
                            buffered = player.bufferedPosition.toDurationMillis(),
                            duration = duration,
                            episodeId = currentEpisodeId(),
                        )
                    } else {
                        null
                    }
                }
                progressValue?.let {
                    publishProgress(it.position, it.buffered, it.duration, it.episodeId)
                }
                delay(500L)
            }

            if (playback == Player.STATE_ENDED) {
                val progressValue = withContext(Dispatchers.Main) {
                    val duration = player.duration.toDurationMillis()
                    if (duration.isPositive()) {
                        Progress(
                            position = duration,
                            buffered = duration,
                            duration = duration,
                            episodeId = currentEpisodeId(),
                        )
                    } else {
                        null
                    }
                }
                progressValue?.let {
                    publishProgress(it.position, it.buffered, it.duration, it.episodeId)
                }
            }
        }
    }

    init {
        player.addListener(listener)
        progressUpdater.launchIn(scope)
    }
}

/** 알림 아트 캐시의 상한. 카탈로그 크기(36) 안팎이라 넘지 않는 안전장치다. */
private const val StationArtworkCacheMax = 36
