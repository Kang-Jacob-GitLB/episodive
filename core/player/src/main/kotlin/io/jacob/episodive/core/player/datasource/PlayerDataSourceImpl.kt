package io.jacob.episodive.core.player.datasource

import androidx.core.net.toUri
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import androidx.media3.common.Tracks
import androidx.media3.common.text.CueGroup
import androidx.media3.exoplayer.ExoPlayer
import io.jacob.episodive.core.model.Episode
import io.jacob.episodive.core.model.Progress
import io.jacob.episodive.core.model.mapper.toDurationMillis
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject
import kotlin.time.Duration

class PlayerDataSourceImpl @Inject constructor(
    private val player: ExoPlayer,
) : PlayerDataSource {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

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
                item.localConfiguration?.tag as? Episode
            }
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

            val episode = mediaItem?.localConfiguration?.tag as? Episode
            _nowPlaying.value = episode
            _indexOfList.value = player.currentMediaItemIndex
            _progress.value = Progress(
                position = Duration.ZERO,
                buffered = Duration.ZERO,
                duration = episode?.duration ?: Duration.ZERO,
            )
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
            when (playbackState) {
                Player.STATE_IDLE -> Timber.d("STATE_IDLE")
                Player.STATE_BUFFERING -> Timber.d("STATE_BUFFERING")
                Player.STATE_READY -> Timber.d("STATE_READY")
                Player.STATE_ENDED -> Timber.d("STATE_ENDED")
            }
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            Timber.d("isPlaying: $isPlaying")
            _isPlaying.value = isPlaying
        }

        override fun onRepeatModeChanged(repeatMode: Int) {
            _repeat.value = repeatMode
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
            .setArtworkUri(image.ifEmpty { feedImage }.toUri())
            .build()

        val uri = if (isDownloaded && filePath != null) {
            android.net.Uri.fromFile(java.io.File(filePath))
        } else {
            enclosureUrl.toUri()
        }

        val builder = MediaItem.Builder()
            .setMediaId(id.toString())
            .setUri(uri)
            .setTag(this)
            .setMediaMetadata(mediaMetadata)

        if (isClip && hasClip) {
            builder.setClippingConfiguration(
                MediaItem.ClippingConfiguration.Builder()
                    .setStartPositionMs(clipStartPositionMs)
                    .setEndPositionMs(clipEndPositionMs)
                    .build()
            )
        }

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

    override fun getPlayer(): Player {
        return player
    }

    // Intentionally only calls prepare() without play() — restores the player in a paused state
    // so the user can choose when to start playback after app restart.
    override fun prepare(episodes: List<Episode>, indexToPlay: Int, positionMs: Long) {
        val mediaItems = episodes.map { it.toMediaItem(isClip = false) }
        player.setMediaItems(mediaItems, indexToPlay, positionMs)
        player.prepare()
    }

    override fun play(episode: Episode) {
        Timber.i("url: ${episode.enclosureUrl}")
        val mediaItem = episode.toMediaItem(isClip = false)

        player.setMediaItem(mediaItem)
        player.prepare()
        player.playWhenReady = true
    }

    override fun play(episodes: List<Episode>, indexToPlay: Int?) {
        episodes.forEachIndexed { index, episode ->
            Timber.i("[$index] url: ${episode.enclosureUrl}")
        }
        val mediaItems = episodes.map { it.toMediaItem(isClip = false) }

        player.setMediaItems(mediaItems)
        indexToPlay?.let { player.seekToDefaultPosition(it) }
        player.prepare()
        player.playWhenReady = true
    }

    override fun playClip(episode: Episode) {
        Timber.i("url: ${episode.enclosureUrl}, clipStartTime: ${episode.clipStartTime}, clipDuration: ${episode.clipDuration}")
        val mediaItem = episode.toMediaItem(isClip = true)

        player.setMediaItem(mediaItem)
        player.prepare()
        player.playWhenReady = true
    }

    override fun playClips(episodes: List<Episode>, indexToPlay: Int?) {
        episodes.forEachIndexed { index, episode ->
            Timber.i("[$index] url: ${episode.enclosureUrl}, clipStartTime: ${episode.clipStartTime}, clipDuration: ${episode.clipDuration}")
        }
        val mediaItems = episodes.map { it.toMediaItem(isClip = true) }

        player.setMediaItems(mediaItems)
        indexToPlay?.let { player.seekToDefaultPosition(it) }
        player.prepare()
        player.playWhenReady = true
    }

    override fun playIndex(index: Int) {
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
        player.stop()
        player.clearMediaItems()
    }

    override fun next() {
        if (player.hasNextMediaItem()) {
            player.seekToNextMediaItem()
            player.playWhenReady = true
        }
    }

    override fun previous() {
        if (player.hasPreviousMediaItem()) {
            player.seekToPrevious()
            player.playWhenReady = true
        }
    }

    override fun seekTo(position: Long) {
        player.seekTo(position)
        _progress.value = Progress(
            position = position.toDurationMillis(),
            buffered = player.bufferedPosition.toDurationMillis(),
            duration = player.duration.toDurationMillis(),
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
        player.repeatMode = repeat
    }

    override fun changeRepeat() {
        val newState = when (player.repeatMode) {
            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ONE
            Player.REPEAT_MODE_ONE -> Player.REPEAT_MODE_ALL
            Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_OFF
            else -> return
        }
        player.repeatMode = newState
    }

    override fun setSpeed(speed: Float) {
        val safeSpeed = speed.coerceIn(0.5f, 3.5f)
        player.setPlaybackSpeed(safeSpeed)
        _speed.value = safeSpeed
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
        // 같은 episode 면 무시 (불필요한 widget 갱신/depounce 방지).
        if (_nowPlaying.value?.id == episode.id) return
        _nowPlaying.value = episode
        _isPlaying.value = player.isPlaying
    }

    private val _nowPlaying = MutableStateFlow<Episode?>(null)
    override val nowPlaying: Flow<Episode?> = _nowPlaying

    private val _playlist = MutableStateFlow<List<Episode>>(emptyList())
    override val playlist: Flow<List<Episode>> = _playlist

    private val _indexOfList = MutableStateFlow(0)
    override val indexOfList: Flow<Int> = _indexOfList

    private val _playback = MutableStateFlow(Player.STATE_IDLE)
    override val playback: Flow<Int> = _playback

    private val _isPlaying = MutableStateFlow(false)
    override val isPlaying: Flow<Boolean> = _isPlaying

    private val _isShuffle = MutableStateFlow(false)
    override val isShuffle: Flow<Boolean> = _isShuffle

    private val _repeat = MutableStateFlow(Player.REPEAT_MODE_OFF)
    override val repeat: Flow<Int> = _repeat

    private val _speed = MutableStateFlow(1.0f)
    override val speed: Flow<Float> = _speed

    private val _cue = MutableStateFlow("")
    override val cue: Flow<String> = _cue

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
                val progressValue = withContext(Dispatchers.Main) {
                    val duration = player.duration.toDurationMillis()
                    if (duration.isPositive()) {
                        Progress(
                            position = player.currentPosition.toDurationMillis(),
                            buffered = player.bufferedPosition.toDurationMillis(),
                            duration = duration,
                        )
                    } else {
                        null
                    }
                }
                progressValue?.let { _progress.value = it }
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
                        )
                    } else {
                        null
                    }
                }
                progressValue?.let { _progress.value = it }
            }
        }
    }

    init {
        player.addListener(listener)
        progressUpdater.launchIn(scope)
    }
}
