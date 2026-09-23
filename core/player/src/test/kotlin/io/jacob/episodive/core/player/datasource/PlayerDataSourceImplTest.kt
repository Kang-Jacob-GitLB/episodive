package io.jacob.episodive.core.player.datasource

import android.net.Uri
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import androidx.media3.common.Tracks
import androidx.media3.common.text.Cue
import androidx.media3.common.text.CueGroup
import androidx.media3.exoplayer.ExoPlayer
import app.cash.turbine.test
import io.jacob.episodive.core.domain.download.EpisodeDownloader
import io.jacob.episodive.core.model.Episode
import io.jacob.episodive.core.model.mapper.toDurationMillis
import io.jacob.episodive.core.player.audio.PlaybackSpectrumMonitor
import io.jacob.episodive.core.testing.model.episodeTestData
import io.jacob.episodive.core.testing.model.episodeTestDataList
import io.mockk.Runs
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

class PlayerDataSourceImplTest {
    private val player = mockk<ExoPlayer>(relaxed = true)
    private val episodeDownloader = mockk<EpisodeDownloader>(relaxed = true)
    private val listenerSlot = slot<Player.Listener>()

    private lateinit var dataSource: PlayerDataSourceImpl

    private val episode = episodeTestData

    @Before
    fun setUp() {
        mockkStatic(Uri::class)
        every { Uri.parse(any()) } returns mockk(relaxed = true)
        every { Uri.fromFile(any()) } returns mockk(relaxed = true)

        every { player.addListener(capture(listenerSlot)) } just Runs

        dataSource = PlayerDataSourceImpl(player, episodeDownloader)
    }

    @After
    fun tearDown() {
        dataSource.release()
        unmockkStatic(Uri::class)
    }

    // ---------------------------------------------------------------------
    // getPlayer / prepare / play / playClip / playClips
    // ---------------------------------------------------------------------

    @Test
    fun `Given player, When getPlayer called, Then same player instance returned`() {
        // When
        val result = dataSource.getPlayer()

        // Then
        assertEquals(player, result)
    }

    @Test
    fun `Given episodes, When prepare called, Then setMediaItems and prepare called on player`() {
        // Given
        val episodes = listOf(episode)

        // When
        dataSource.prepare(episodes, indexToPlay = 0, positionMs = 1000L)

        // Then
        verify {
            player.setMediaItems(any(), 0, 1000L)
            player.prepare()
        }
    }

    @Test
    fun `Given episode, When play called, Then setMediaItem, prepare, playWhenReady set on player`() {
        // When
        dataSource.play(episode)

        // Then
        val slot = slot<MediaItem>()
        verify {
            player.setMediaItem(capture(slot))
            player.prepare()
            player.playWhenReady = true
        }
        assertEquals(episode.id.toString(), slot.captured.mediaId)
        assertEquals(episode, slot.captured.localConfiguration?.tag)
    }

    @Test
    fun `Given episode with downloaded file, When play called, Then local file path resolved from downloader`() {
        // Given
        val downloadedEpisode = episode.copy(filePath = "5778530/42551776753.mp3")
        every { episodeDownloader.isFileDownloaded("5778530/42551776753.mp3") } returns true
        every { episodeDownloader.getDownloadDirectory() } returns "/data/downloads"

        // When
        dataSource.play(downloadedEpisode)

        // Then
        verify {
            episodeDownloader.isFileDownloaded("5778530/42551776753.mp3")
            episodeDownloader.getDownloadDirectory()
            player.setMediaItem(any())
        }
    }

    @Test
    fun `Given episodes and index, When play list called, Then setMediaItems, seekToDefaultPosition, prepare called`() {
        // Given
        val episodes = listOf(episode, episode.copy(id = 999L))

        // When
        dataSource.play(episodes, indexToPlay = 1)

        // Then
        verify {
            player.setMediaItems(any())
            player.seekToDefaultPosition(1)
            player.prepare()
            player.playWhenReady = true
        }
    }

    @Test
    fun `Given episodes and null index, When play list called, Then seekToDefaultPosition not called`() {
        // Given
        val episodes = listOf(episode)

        // When
        dataSource.play(episodes, indexToPlay = null)

        // Then
        verify(exactly = 0) { player.seekToDefaultPosition(any()) }
        verify {
            player.setMediaItems(any())
            player.prepare()
            player.playWhenReady = true
        }
    }

    @Test
    fun `Given clip episode, When playClip called, Then setMediaItem with clipping configuration applied`() {
        // Given: 클립 픽스처는 아래의 clipEpisode 프로퍼티 하나만 쓴다. 여기서 같은 이름의
        // 지역 변수를 따로 만들면 프로퍼티를 가려, 그쪽을 손봐도 이 테스트는 꿈쩍하지 않는다.

        // When
        dataSource.playClip(clipEpisode)

        // Then
        val slot = slot<MediaItem>()
        verify {
            player.setMediaItem(capture(slot))
            player.prepare()
            player.playWhenReady = true
        }
        assertEquals(clipEpisode.clipStartPositionMs, slot.captured.clippingConfiguration.startPositionMs)
        assertEquals(clipEpisode.clipEndPositionMs, slot.captured.clippingConfiguration.endPositionMs)
    }

    @Test
    fun `Given clip episodes and index, When playClips called, Then setMediaItems, seekToDefaultPosition, prepare called`() {
        // Given
        val episodes = listOf(episode, episode.copy(id = 999L))

        // When
        dataSource.playClips(episodes, indexToPlay = 1)

        // Then
        verify {
            player.setMediaItems(any())
            player.seekToDefaultPosition(1)
            player.prepare()
            player.playWhenReady = true
        }
    }

    @Test
    fun `Given episode with transcriptUrl, When play called, Then subtitle configuration applied`() {
        // Given
        val episodeWithTranscript = episode.copy(transcriptUrl = "https://example.com/transcript.vtt")

        // When
        dataSource.play(episodeWithTranscript)

        // Then
        val slot = slot<MediaItem>()
        verify { player.setMediaItem(capture(slot)) }
        assertEquals(1, slot.captured.localConfiguration?.subtitleConfigurations?.size)
    }

    // ---------------------------------------------------------------------
    // playIndex / playOrPause / pause / resume / stop / next / previous
    // ---------------------------------------------------------------------

    @Test
    fun `Given valid index, When playIndex called, Then seekToDefaultPosition and playWhenReady set`() {
        // Given
        every { player.mediaItemCount } returns 5

        // When
        dataSource.playIndex(2)

        // Then
        verify {
            player.seekToDefaultPosition(2)
            player.playWhenReady = true
        }
    }

    @Test
    fun `Given out-of-range index, When playIndex called, Then player not touched`() {
        // Given
        every { player.mediaItemCount } returns 3

        // When
        dataSource.playIndex(5)

        // Then
        verify(exactly = 0) { player.seekToDefaultPosition(any()) }
    }

    @Test
    fun `Given player is playing, When playOrPause called, Then pause called`() {
        // Given
        every { player.isPlaying } returns true

        // When
        dataSource.playOrPause()

        // Then
        verify { player.pause() }
        verify(exactly = 0) { player.play() }
    }

    @Test
    fun `Given player is not playing, When playOrPause called, Then play called`() {
        // Given
        every { player.isPlaying } returns false

        // When
        dataSource.playOrPause()

        // Then
        verify { player.play() }
        verify(exactly = 0) { player.pause() }
    }

    @Test
    fun `Given player is playing, When pause called, Then player pause invoked`() {
        // Given
        every { player.isPlaying } returns true

        // When
        dataSource.pause()

        // Then
        verify { player.pause() }
    }

    @Test
    fun `Given player is not playing, When pause called, Then player pause not invoked`() {
        // Given
        every { player.isPlaying } returns false

        // When
        dataSource.pause()

        // Then
        verify(exactly = 0) { player.pause() }
    }

    @Test
    fun `Given player is not playing, When resume called, Then player play invoked`() {
        // Given
        every { player.isPlaying } returns false

        // When
        dataSource.resume()

        // Then
        verify { player.play() }
    }

    @Test
    fun `Given player is playing, When resume called, Then player play not invoked`() {
        // Given
        every { player.isPlaying } returns true

        // When
        dataSource.resume()

        // Then
        verify(exactly = 0) { player.play() }
    }

    @Test
    fun `When stop called, Then player stop and clearMediaItems invoked`() {
        // When
        dataSource.stop()

        // Then
        verify {
            player.stop()
            player.clearMediaItems()
        }
    }

    @Test
    fun `Given hasNextMediaItem true, When next called, Then seekToNextMediaItem and playWhenReady set`() {
        // Given
        every { player.hasNextMediaItem() } returns true

        // When
        dataSource.next()

        // Then
        verify {
            player.seekToNextMediaItem()
            player.playWhenReady = true
        }
    }

    @Test
    fun `Given hasNextMediaItem false, When next called, Then player not touched`() {
        // Given
        every { player.hasNextMediaItem() } returns false

        // When
        dataSource.next()

        // Then
        verify(exactly = 0) { player.seekToNextMediaItem() }
    }

    @Test
    fun `Given hasPreviousMediaItem true, When previous called, Then seekToPrevious and playWhenReady set`() {
        // Given
        every { player.hasPreviousMediaItem() } returns true

        // When
        dataSource.previous()

        // Then
        verify {
            player.seekToPrevious()
            player.playWhenReady = true
        }
    }

    @Test
    fun `Given hasPreviousMediaItem false, When previous called, Then player not touched`() {
        // Given
        every { player.hasPreviousMediaItem() } returns false

        // When
        dataSource.previous()

        // Then
        verify(exactly = 0) { player.seekToPrevious() }
    }

    // ---------------------------------------------------------------------
    // seek / shuffle / repeat / speed / volume
    // ---------------------------------------------------------------------

    @Test
    fun `Given position, When seekTo called, Then player seekTo invoked`() {
        // Given
        every { player.bufferedPosition } returns 2000L
        every { player.duration } returns 10000L

        // When
        dataSource.seekTo(1000L)

        // Then
        verify { player.seekTo(1000L) }
    }

    @Test
    fun `When seekBackward called, Then player seekBack invoked`() {
        // When
        dataSource.seekBackward()

        // Then
        verify { player.seekBack() }
    }

    @Test
    fun `When seekForward called, Then player seekForward invoked`() {
        // When
        dataSource.seekForward()

        // Then
        verify { player.seekForward() }
    }

    @Test
    fun `Given isShuffle true, When setShuffle called, Then shuffleModeEnabled set`() {
        // When
        dataSource.setShuffle(true)

        // Then
        verify { player.shuffleModeEnabled = true }
    }

    @Test
    fun `Given shuffleModeEnabled false, When shuffle called, Then shuffleModeEnabled toggled to true`() {
        // Given
        every { player.shuffleModeEnabled } returns false

        // When
        dataSource.shuffle()

        // Then
        verify { player.shuffleModeEnabled = true }
    }

    @Test
    fun `Given shuffleModeEnabled true, When shuffle called, Then shuffleModeEnabled toggled to false`() {
        // Given
        every { player.shuffleModeEnabled } returns true

        // When
        dataSource.shuffle()

        // Then
        verify { player.shuffleModeEnabled = false }
    }

    @Test
    fun `Given repeat mode, When setRepeat called, Then player repeatMode set`() {
        // When
        dataSource.setRepeat(Player.REPEAT_MODE_ALL)

        // Then
        verify { player.repeatMode = Player.REPEAT_MODE_ALL }
    }

    @Test
    fun `Given repeat mode OFF, When changeRepeat called, Then repeatMode changed to ONE`() {
        // Given
        every { player.repeatMode } returns Player.REPEAT_MODE_OFF

        // When
        dataSource.changeRepeat()

        // Then
        verify { player.repeatMode = Player.REPEAT_MODE_ONE }
    }

    @Test
    fun `Given repeat mode ONE, When changeRepeat called, Then repeatMode changed to ALL`() {
        // Given
        every { player.repeatMode } returns Player.REPEAT_MODE_ONE

        // When
        dataSource.changeRepeat()

        // Then
        verify { player.repeatMode = Player.REPEAT_MODE_ALL }
    }

    @Test
    fun `Given repeat mode ALL, When changeRepeat called, Then repeatMode changed to OFF`() {
        // Given
        every { player.repeatMode } returns Player.REPEAT_MODE_ALL

        // When
        dataSource.changeRepeat()

        // Then
        verify { player.repeatMode = Player.REPEAT_MODE_OFF }
    }

    @Test
    fun `Given speed within range, When setSpeed called, Then player playback speed set as given`() {
        // When
        dataSource.setSpeed(1.5f)

        // Then
        verify { player.setPlaybackSpeed(1.5f) }
    }

    @Test
    fun `Given speed above max, When setSpeed called, Then player playback speed coerced to max`() {
        // When
        dataSource.setSpeed(10f)

        // Then
        verify { player.setPlaybackSpeed(3.5f) }
    }

    @Test
    fun `Given speed below min, When setSpeed called, Then player playback speed coerced to min`() {
        // When
        dataSource.setSpeed(0f)

        // Then
        verify { player.setPlaybackSpeed(0.5f) }
    }

    @Test
    fun `Given volume within range, When setVolume called, Then player volume set as given`() {
        // When
        dataSource.setVolume(0.5f)

        // Then
        verify { player.volume = 0.5f }
    }

    @Test
    fun `Given volume above max, When setVolume called, Then player volume coerced to 1`() {
        // When
        dataSource.setVolume(2f)

        // Then
        verify { player.volume = 1f }
    }

    @Test
    fun `Given volume below min, When setVolume called, Then player volume coerced to 0`() {
        // When
        dataSource.setVolume(-1f)

        // Then
        verify { player.volume = 0f }
    }

    // ---------------------------------------------------------------------
    // addTrack / addClipTrack / removeTrack / clearPlayList
    // ---------------------------------------------------------------------

    @Test
    fun `Given episode and index, When addTrack called, Then addMediaItem with index invoked`() {
        // When
        dataSource.addTrack(episode, index = 1)

        // Then
        verify { player.addMediaItem(1, any()) }
    }

    @Test
    fun `Given episode and null index, When addTrack called, Then addMediaItem without index invoked`() {
        // When
        dataSource.addTrack(episode, index = null)

        // Then
        verify { player.addMediaItem(any<MediaItem>()) }
        verify(exactly = 0) { player.addMediaItem(any(), any()) }
    }

    @Test
    fun `Given episodes and index, When addTrack list called, Then addMediaItems with index invoked`() {
        // Given
        val episodes = listOf(episode, episode.copy(id = 999L))

        // When
        dataSource.addTrack(episodes, index = 0)

        // Then
        verify { player.addMediaItems(0, any()) }
    }

    @Test
    fun `Given episodes and null index, When addTrack list called, Then addMediaItems without index invoked`() {
        // Given
        val episodes = listOf(episode)

        // When
        dataSource.addTrack(episodes, index = null)

        // Then
        verify { player.addMediaItems(any<List<MediaItem>>()) }
    }

    @Test
    fun `Given clip episode and index, When addClipTrack called, Then addMediaItem with index invoked`() {
        // When
        dataSource.addClipTrack(episode, index = 2)

        // Then
        verify { player.addMediaItem(2, any()) }
    }

    @Test
    fun `Given clip episode and null index, When addClipTrack called, Then addMediaItem without index invoked`() {
        // When
        dataSource.addClipTrack(episode, index = null)

        // Then
        verify { player.addMediaItem(any<MediaItem>()) }
        verify(exactly = 0) { player.addMediaItem(any(), any()) }
    }

    @Test
    fun `Given clip episodes and index, When addClipTracks called, Then addMediaItems with index invoked`() {
        // Given
        val episodes = listOf(episode, episode.copy(id = 999L))

        // When
        dataSource.addClipTracks(episodes, index = 0)

        // Then
        verify { player.addMediaItems(0, any()) }
    }

    @Test
    fun `Given clip episodes and null index, When addClipTracks called, Then addMediaItems without index invoked`() {
        // Given
        val episodes = listOf(episode)

        // When
        dataSource.addClipTracks(episodes, index = null)

        // Then
        verify { player.addMediaItems(any<List<MediaItem>>()) }
    }

    @Test
    fun `Given index, When removeTrack called, Then removeMediaItem invoked`() {
        // When
        dataSource.removeTrack(3)

        // Then
        verify { player.removeMediaItem(3) }
    }

    @Test
    fun `When clearPlayList called, Then clearMediaItems invoked`() {
        // When
        dataSource.clearPlayList()

        // Then
        verify { player.clearMediaItems() }
    }

    // ---------------------------------------------------------------------
    // release / rehydrate
    // ---------------------------------------------------------------------

    @Test
    fun `When release called, Then player release and removeListener invoked`() {
        // When
        dataSource.release()

        // Then
        verify {
            player.release()
            player.removeListener(any())
        }
    }

    @Test
    fun `Given no current nowPlaying, When rehydrate called, Then nowPlaying and isPlaying updated`() = runTest {
        // Given: isPlaying kept false to avoid triggering the real-time progressUpdater
        // coroutine (see PlayerDataSourceImplTest class doc / task notes: progressUpdater is
        // intentionally not exercised here).
        every { player.isPlaying } returns false

        // When / Then
        dataSource.nowPlaying.test {
            assertEquals(null, awaitItem())
            dataSource.rehydrate(episode)
            assertEquals(episode, awaitItem())
        }
        dataSource.isPlaying.test {
            assertEquals(false, awaitItem())
        }
    }

    @Test
    fun `Given same episode already playing, When rehydrate called, Then nowPlaying flow not re-emitted`() = runTest {
        // Given
        every { player.isPlaying } returns false
        dataSource.rehydrate(episode)

        // When / Then: emits once for the first rehydrate, calling again with same id should not add new emission
        dataSource.nowPlaying.test {
            assertEquals(episode, awaitItem())
            dataSource.rehydrate(episode)
            expectNoEvents()
        }
    }

    // ---------------------------------------------------------------------
    // Player.Listener callbacks
    // ---------------------------------------------------------------------

    @Test
    fun `Given media items in player, When onTimelineChanged invoked, Then playlist flow updated with episodes`() = runTest {
        // Given
        val mediaItem = MediaItem.Builder()
            .setMediaId(episode.id.toString())
            .setUri(mockk<Uri>(relaxed = true))
            .setTag(episode)
            .build()
        every { player.mediaItemCount } returns 1
        every { player.getMediaItemAt(0) } returns mediaItem

        // When
        listenerSlot.captured.onTimelineChanged(
            mockk<Timeline>(relaxed = true),
            Player.TIMELINE_CHANGE_REASON_PLAYLIST_CHANGED,
        )

        // Then
        dataSource.playlist.test {
            assertEquals(listOf(episode), awaitItem())
        }
    }

    @Test
    fun `Given media item transition, When onMediaItemTransition invoked, Then nowPlaying indexOfList progress updated`() = runTest {
        // Given
        val mediaItem = MediaItem.Builder()
            .setMediaId(episode.id.toString())
            .setUri(mockk<Uri>(relaxed = true))
            .setTag(episode)
            .build()
        every { player.currentMediaItemIndex } returns 3

        // When
        listenerSlot.captured.onMediaItemTransition(
            mediaItem,
            Player.MEDIA_ITEM_TRANSITION_REASON_AUTO,
        )

        // Then
        dataSource.nowPlaying.test { assertEquals(episode, awaitItem()) }
        dataSource.indexOfList.test { assertEquals(3, awaitItem()) }
        dataSource.progress.test {
            val progress = awaitItem()
            assertEquals(episode.duration, progress.duration)
            // T7 (C1/C2 회귀): transition 이 실은 progress 는 반드시 전환된 에피소드의 id 를
            // 함께 싣고, 위치는 0 으로 리셋되어야 한다 (이전 에피소드의 위치가 새 에피소드로 새는 것 방지).
            assertEquals(episode.id, progress.episodeId)
            assertEquals(Duration.ZERO, progress.position)
        }
    }

    @Test
    fun `Given seek driven transition, When onMediaItemTransition invoked, Then progress position comes from player currentPosition`() = runTest {
        // 탐색으로 항목이 바뀐 경우엔 플레이어가 이미 목표 위치에 가 있으므로 그 값을 싣는다.
        // 여기서 0 을 하드코딩하면 방금 이동한 지점이 곧바로 0 으로 저장된다.
        val mediaItem = MediaItem.Builder()
            .setMediaId(episode.id.toString())
            .setUri(mockk<Uri>(relaxed = true))
            .setTag(episode)
            .build()
        every { player.currentMediaItemIndex } returns 0
        every { player.currentPosition } returns 45_000L

        // When
        listenerSlot.captured.onMediaItemTransition(
            mediaItem,
            Player.MEDIA_ITEM_TRANSITION_REASON_SEEK,
        )

        // Then
        dataSource.progress.test {
            val progress = awaitItem()
            assertEquals(45_000L.toDurationMillis(), progress.position)
            assertEquals(episode.id, progress.episodeId)
        }
    }

    @Test
    fun `Given repeat transition, When onMediaItemTransition invoked, Then progress position is zero`() = runTest {
        // 한 곡 반복도 같은 항목을 처음부터 다시 재생하므로 0 이 확정이다.
        // AUTO 와 같은 분기를 공유하지만, 누군가 REPEAT 만 else 로 옮기면 잡아낸다.
        val mediaItem = MediaItem.Builder()
            .setMediaId(episode.id.toString())
            .setUri(mockk<Uri>(relaxed = true))
            .setTag(episode)
            .build()
        every { player.currentMediaItemIndex } returns 0
        every { player.currentPosition } returns 3_500_000L

        // When
        listenerSlot.captured.onMediaItemTransition(
            mediaItem,
            Player.MEDIA_ITEM_TRANSITION_REASON_REPEAT,
        )

        // Then
        dataSource.progress.test {
            val progress = awaitItem()
            assertEquals(Duration.ZERO, progress.position)
            assertEquals(episode.id, progress.episodeId)
        }
    }

    @Test
    fun `Given auto transition to next item, When onMediaItemTransition invoked, Then progress position is zero not the previous item position`() = runTest {
        // 자동 전환은 새 항목을 처음부터 재생한다. 이 시점의 player.currentPosition 이 아직
        // 이전 항목의 끝 위치일 수 있는데, 그 값을 새 에피소드에 실으면 듣지도 않은
        // 에피소드가 사실상 완료 처리되어 이어듣기 지점이 사라진다.
        val mediaItem = MediaItem.Builder()
            .setMediaId(episode.id.toString())
            .setUri(mockk<Uri>(relaxed = true))
            .setTag(episode)
            .build()
        every { player.currentMediaItemIndex } returns 1
        // 이전 항목의 끝 무렵 위치가 그대로 보이는 상황을 재현한다.
        every { player.currentPosition } returns 3_500_000L

        // When
        listenerSlot.captured.onMediaItemTransition(
            mediaItem,
            Player.MEDIA_ITEM_TRANSITION_REASON_AUTO,
        )

        // Then
        dataSource.progress.test {
            val progress = awaitItem()
            assertEquals(Duration.ZERO, progress.position)
            assertEquals(episode.id, progress.episodeId)
        }
    }

    @Test
    fun `Given null media item, When onMediaItemTransition invoked, Then nowPlaying set to null`() = runTest {
        // When
        listenerSlot.captured.onMediaItemTransition(
            null,
            Player.MEDIA_ITEM_TRANSITION_REASON_PLAYLIST_CHANGED,
        )

        // Then
        dataSource.nowPlaying.test { assertEquals(null, awaitItem()) }
    }

    @Test
    fun `Given playback state, When onPlaybackStateChanged invoked, Then playback flow updated`() = runTest {
        // When
        listenerSlot.captured.onPlaybackStateChanged(Player.STATE_READY)

        // Then
        dataSource.playback.test { assertEquals(Player.STATE_READY, awaitItem()) }
    }

    @Test
    fun `Given repeat mode, When onRepeatModeChanged invoked, Then repeat flow updated`() = runTest {
        // When
        listenerSlot.captured.onRepeatModeChanged(Player.REPEAT_MODE_ONE)

        // Then
        dataSource.repeat.test { assertEquals(Player.REPEAT_MODE_ONE, awaitItem()) }
    }

    @Test
    fun `Given shuffle flag, When onShuffleModeEnabledChanged invoked, Then isShuffle flow updated`() = runTest {
        // When
        listenerSlot.captured.onShuffleModeEnabledChanged(true)

        // Then
        dataSource.isShuffle.test { assertEquals(true, awaitItem()) }
    }

    @Test
    fun `Given playback exception, When onPlayerError invoked, Then no exception thrown`() {
        // Given
        val error = mockk<PlaybackException>(relaxed = true)

        // When / Then (should not throw)
        listenerSlot.captured.onPlayerError(error)
    }

    @Test
    fun `Given tracks with text track, When onTracksChanged invoked, Then no exception thrown`() {
        // Given
        val trackGroup = mockk<Tracks.Group>(relaxed = true) {
            every { type } returns C.TRACK_TYPE_TEXT
        }
        val tracks = mockk<Tracks>(relaxed = true) {
            every { groups } returns com.google.common.collect.ImmutableList.of(trackGroup)
        }

        // When / Then (should not throw)
        listenerSlot.captured.onTracksChanged(tracks)
    }

    @Test
    fun `Given cue group with text, When onCues invoked, Then cue flow updated`() = runTest {
        // Given
        val cue = Cue.Builder().setText("hello").build()
        val cueGroup = CueGroup(listOf(cue), 0L)

        // When
        listenerSlot.captured.onCues(cueGroup)

        // Then
        dataSource.cue.test { assertEquals("hello", awaitItem()) }
    }

    @Test
    fun `Given empty cue group, When onCues invoked, Then cue flow updated with empty string`() = runTest {
        // Given
        val cueGroup = CueGroup(emptyList(), 0L)

        // When
        listenerSlot.captured.onCues(cueGroup)

        // Then
        dataSource.cue.test { assertEquals("", awaitItem()) }
    }

    @Test
    fun `Given a seek discontinuity, When onPositionDiscontinuity invoked, Then seeks emits`() = runTest {
        val oldPosition = mockk<Player.PositionInfo>(relaxed = true)
        val newPosition = mockk<Player.PositionInfo>(relaxed = true)

        dataSource.seeks.test {
            listenerSlot.captured.onPositionDiscontinuity(oldPosition, newPosition, Player.DISCONTINUITY_REASON_SEEK)
            awaitItem()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `Given a seek adjustment discontinuity, When onPositionDiscontinuity invoked, Then seeks emits`() = runTest {
        val oldPosition = mockk<Player.PositionInfo>(relaxed = true)
        val newPosition = mockk<Player.PositionInfo>(relaxed = true)

        dataSource.seeks.test {
            listenerSlot.captured.onPositionDiscontinuity(
                oldPosition,
                newPosition,
                Player.DISCONTINUITY_REASON_SEEK_ADJUSTMENT,
            )
            awaitItem()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `Given a non-seek discontinuity, When onPositionDiscontinuity invoked, Then seeks does not emit`() = runTest {
        val oldPosition = mockk<Player.PositionInfo>(relaxed = true)
        val newPosition = mockk<Player.PositionInfo>(relaxed = true)

        dataSource.seeks.test {
            listenerSlot.captured.onPositionDiscontinuity(
                oldPosition,
                newPosition,
                Player.DISCONTINUITY_REASON_AUTO_TRANSITION,
            )
            expectNoEvents()
        }
    }

    @Test
    fun `Given media metadata, When onMediaMetadataChanged invoked, Then no exception thrown`() {
        // Given
        val metadata = MediaMetadata.Builder().setTitle("title").build()

        // When / Then (should not throw)
        listenerSlot.captured.onMediaMetadataChanged(metadata)
    }

    @Test
    fun `Given isPlaying flag, When onIsPlayingChanged invoked, Then isPlaying flow updated`() = runTest {
        // When
        listenerSlot.captured.onIsPlayingChanged(false)

        // Then
        dataSource.isPlaying.test { assertEquals(false, awaitItem()) }
    }

    // ---------------------------------------------------------------------
    // C1~C4 회귀 테스트: 재생 위치 오염 방지
    // ---------------------------------------------------------------------

    @Test
    fun `Given media3 fires transition inline inside setMediaItems, When prepare called, Then final progress keeps restored position and episodeId`() = runTest {
        // Given: media3 1.8.0 은 setMediaItems 호출 스택 안에서 onMediaItemTransition 을
        // 인라인 실행한다(C4). 그 콜백이 position 을 0 으로 덮은 뒤에도, prepare() 가 그 뒤에서
        // 실제 복원 위치로 다시 확정해야 한다.
        val mediaItem = MediaItem.Builder()
            .setMediaId(episode.id.toString())
            .setUri(mockk<Uri>(relaxed = true))
            .setTag(episode)
            .build()
        every {
            player.setMediaItems(any(), any<Int>(), any<Long>())
        } answers {
            listenerSlot.captured.onMediaItemTransition(
                mediaItem,
                Player.MEDIA_ITEM_TRANSITION_REASON_PLAYLIST_CHANGED,
            )
        }

        // When
        dataSource.prepare(listOf(episode), indexToPlay = 0, positionMs = 60_000L)

        // Then
        dataSource.progress.test {
            val progress = awaitItem()
            assertEquals(60_000L.toDurationMillis(), progress.position)
            assertEquals(episode.id, progress.episodeId)
        }
    }

    @Test
    fun `Given media3 fires transition inline during prepare, When isPreparing guard is active, Then no progress is published mid setMediaItems callback`() = runTest {
        // Given: (B) isPreparing 가드가 실제로 콜백 도중 발행을 막는지 직접 확인한다.
        // progress 는 StateFlow 라 구독 시점에 따라(특히 단일 스레드 테스트 스케줄러에서는)
        // 중간 방출이 컨플레이션으로 사라질 수 있다 — Turbine 으로 뒤늦게 구독하면 최종값만
        // 보이는 게 그 예다. 대신 콜백이 실행되는 바로 그 순간의 값을 직접 스냅샷한다.
        val mediaItem = MediaItem.Builder()
            .setMediaId(episode.id.toString())
            .setUri(mockk<Uri>(relaxed = true))
            .setTag(episode)
            .build()
        var progressDuringCallback: io.jacob.episodive.core.model.Progress? = null
        every {
            player.setMediaItems(any(), any<Int>(), any<Long>())
        } answers {
            listenerSlot.captured.onMediaItemTransition(
                mediaItem,
                Player.MEDIA_ITEM_TRANSITION_REASON_PLAYLIST_CHANGED,
            )
            // isPreparing 가드가 없으면 이 시점에 이미 (episodeId=episode.id, position=0) 이
            // 발행되어 있다 — Unconfined 로 즉시(비동기 스케줄링 없이) 현재 값을 읽는다.
            progressDuringCallback = kotlinx.coroutines.runBlocking(kotlinx.coroutines.Dispatchers.Unconfined) {
                dataSource.progress.first()
            }
        }

        // When
        dataSource.prepare(listOf(episode), indexToPlay = 0, positionMs = 60_000L)

        // Then: 콜백이 실행되는 순간에는 아직 아무것도 발행되지 않아야 한다(episodeId=null 유지).
        assertEquals(null, progressDuringCallback?.episodeId)

        // 그리고 prepare() 가 끝난 뒤에는 복원 위치가 최종적으로 확정되어 있어야 한다.
        dataSource.progress.test {
            val progress = awaitItem()
            assertEquals(60_000L.toDurationMillis(), progress.position)
            assertEquals(episode.id, progress.episodeId)
        }
    }

    @Test
    fun `Given player duration is TIME_UNSET, When seekTo called, Then progress duration is not negative`() = runTest {
        // Given: 아직 아무 항목도 올리지 않은 상태. 길이를 알 방법이 전혀 없을 때도
        // TIME_UNSET(음수)이 그대로 새어 나가면 안 된다.
        every { player.duration } returns C.TIME_UNSET
        every { player.bufferedPosition } returns 0L
        every { player.currentMediaItem } returns null

        // When
        dataSource.seekTo(1000L)

        // Then
        dataSource.progress.test {
            val progress = awaitItem()
            assertEquals(false, progress.duration.isNegative())
        }
    }

    @Test
    fun `Given player already holds a different episode, When rehydrate called, Then nowPlaying keeps the playing episode`() = runTest {
        // Given: C3 회귀 방지. rehydrate 의 가드(player.currentMediaItem 기준)가 없으면
        // 비동기 DB 복원값(A)이 실제로 재생 중인 B 를 밀어내고 nowPlaying 을 오염시킨다.
        every { player.isPlaying } returns false
        val otherEpisode = episode.copy(id = 999L)

        // player.currentMediaItem 이 아직 비어 있는 상태에서 B 를 먼저 rehydrate 해
        // nowPlaying 을 B 로 세팅한다.
        dataSource.rehydrate(otherEpisode)

        // 이제 플레이어가 실제로 B 를 들고 있는 상태를 재현한다.
        val otherMediaItem = MediaItem.Builder()
            .setMediaId(otherEpisode.id.toString())
            .setUri(mockk<Uri>(relaxed = true))
            .setTag(otherEpisode)
            .build()
        every { player.currentMediaItem } returns otherMediaItem

        // When: 뒤늦게 도착한 A 로 rehydrate 를 시도한다.
        dataSource.rehydrate(episode)

        // Then: 가드가 없으면 nowPlaying 이 A 로 바뀐다 — 이 assert 가 그걸 잡아낸다.
        dataSource.nowPlaying.test {
            assertEquals(otherEpisode, awaitItem())
        }
    }

    @Test
    fun `Given nowPlaying rehydrated to A while player already holds B, When seekTo called, Then progress episodeId follows the player not nowPlaying`() = runTest {
        // Given: C3 회귀 방지(교차 오염). rehydrate(A) 는 player.currentMediaItem 이 아직 비어
        // 있을 때 허용되므로(가드를 통과) _nowPlaying 이 A 로 세팅될 수 있다. 그 직후 player 가
        // 실제로는 B 를 들고 있는 상태(예: 비동기 전환 콜백이 아직 _nowPlaying 을 못 따라잡은 틈)를
        // 재현한다. seekTo 가 _nowPlaying(=A) 대신 currentEpisodeId()(=B, player 기준) 를 써야만
        // 위치가 B 의 것으로 정확히 저장된다.
        every { player.isPlaying } returns false
        every { player.bufferedPosition } returns 0L
        every { player.duration } returns 600_000L
        val otherEpisode = episode.copy(id = 999L)

        // player.currentMediaItem 이 아직 비어 있는 상태에서 A 를 rehydrate 해 nowPlaying=A.
        dataSource.rehydrate(episode)

        // 실제로는 player 가 B 를 들고 있는 상태로 어긋난다(_nowPlaying 갱신 전).
        val otherMediaItem = MediaItem.Builder()
            .setMediaId(otherEpisode.id.toString())
            .setUri(mockk<Uri>(relaxed = true))
            .setTag(otherEpisode)
            .build()
        every { player.currentMediaItem } returns otherMediaItem

        // When
        dataSource.seekTo(300_000L)

        // Then: episodeId 가 nowPlaying(A) 이 아니라 실제로 player 가 들고 있는 B 여야 한다.
        dataSource.progress.test {
            val progress = awaitItem()
            assertEquals(otherEpisode.id, progress.episodeId)
        }
    }

    // ---------------------------------------------------------------------
    // 3차 수정 회귀 테스트: play/playClips isPreparing 가드 대칭 적용, seekTo duration 폴백
    // ---------------------------------------------------------------------

    @Test
    fun `Given play list with non-first index, When setMediaItems fires inline transition, Then no emission carries first item id`() = runTest {
        // Given: media3 는 1-인자 setMediaItems(list) 호출 스택 안에서도 목록 첫 항목에 대해
        // onMediaItemTransition 을 인라인 실행한다. seekToDefaultPosition(indexToPlay) 로 목표에
        // 도달하기 전이므로, 가드가 없으면 "재생하지도 않을 첫 항목(A) + 위치 0" 이 발행되어
        // A 의 이어듣기 지점이 지워진다.
        val episodeA = episodeTestDataList[0]
        val episodeB = episodeTestDataList[1]
        val itemA = MediaItem.Builder()
            .setMediaId(episodeA.id.toString())
            .setUri(mockk<Uri>(relaxed = true))
            .setTag(episodeA)
            .build()
        every {
            player.setMediaItems(any())
        } answers {
            listenerSlot.captured.onMediaItemTransition(
                itemA,
                Player.MEDIA_ITEM_TRANSITION_REASON_PLAYLIST_CHANGED,
            )
        }

        // When / Then: play() 호출 전에 구독해 모든 방출을 수집한다.
        dataSource.progress.test {
            awaitItem() // 초기값

            dataSource.play(episodes = listOf(episodeA, episodeB), indexToPlay = 1)

            val emissions = cancelAndConsumeRemainingEvents()
                .filterIsInstance<app.cash.turbine.Event.Item<io.jacob.episodive.core.model.Progress>>()
                .map { it.value }

            // 사용자가 요청한 것은 B 인데, 방출 중 A 의 id 가 실린 것이 하나라도 있으면
            // A 의 이어듣기 지점이 지워진다.
            assertEquals(false, emissions.any { it.episodeId == episodeA.id })

            val last = emissions.last()
            assertEquals(episodeB.id, last.episodeId)
            assertEquals(Duration.ZERO, last.position)
        }
    }

    @Test
    fun `Given playClips with non-first index, When setMediaItems fires inline transition, Then no emission carries first item id`() = runTest {
        // playClips 도 play(episodes, indexToPlay) 와 같은 가드를 적용받아야 한다.
        val episodeA = episodeTestDataList[0]
        val episodeB = episodeTestDataList[1]
        val itemA = MediaItem.Builder()
            .setMediaId(episodeA.id.toString())
            .setUri(mockk<Uri>(relaxed = true))
            .setTag(episodeA)
            .build()
        every {
            player.setMediaItems(any())
        } answers {
            listenerSlot.captured.onMediaItemTransition(
                itemA,
                Player.MEDIA_ITEM_TRANSITION_REASON_PLAYLIST_CHANGED,
            )
        }

        dataSource.progress.test {
            awaitItem() // 초기값

            dataSource.playClips(episodes = listOf(episodeA, episodeB), indexToPlay = 1)

            val emissions = cancelAndConsumeRemainingEvents()
                .filterIsInstance<app.cash.turbine.Event.Item<io.jacob.episodive.core.model.Progress>>()
                .map { it.value }

            assertEquals(false, emissions.any { it.episodeId == episodeA.id })

            val last = emissions.last()
            assertEquals(episodeB.id, last.episodeId)
            assertEquals(Duration.ZERO, last.position)
        }
    }

    @Test
    fun `Given progress still carries the previous episode's duration, When seekTo called on the new episode with unset player duration, Then progress duration falls back to the new episode's duration`() = runTest {
        // Given: A 에서 B 로 전환된 직후. player.duration 은 아직 TIME_UNSET 이고, _progress 에는
        // 직전 A 의 duration 이 남아 있다. 이때 _progress.value.duration 으로 폴백하면 A 의 짧은/긴
        // duration 이 그대로 B 에 실려, 짧은 길이 + 큰 위치 조합이 완료 판정을 잘못 뒤집는다.
        val episodeA = episodeTestDataList[0]
        val episodeB = episodeTestDataList[1]
        check(episodeA.duration != episodeB.duration) { "테스트 데이터의 duration 이 같으면 폴백 오류를 구분할 수 없다" }

        // A 로 전환되어 _progress.duration 이 A 의 길이로 채워진 상태를 만든다.
        val itemA = MediaItem.Builder()
            .setMediaId(episodeA.id.toString())
            .setUri(mockk<Uri>(relaxed = true))
            .setTag(episodeA)
            .build()
        every { player.currentMediaItemIndex } returns 0
        listenerSlot.captured.onMediaItemTransition(itemA, Player.MEDIA_ITEM_TRANSITION_REASON_AUTO)

        // 이제 플레이어는 실제로 B 를 들고 있고, 아직 prepare 전이라 duration 이 TIME_UNSET 이다.
        val itemB = MediaItem.Builder()
            .setMediaId(episodeB.id.toString())
            .setUri(mockk<Uri>(relaxed = true))
            .setTag(episodeB)
            .build()
        every { player.currentMediaItem } returns itemB
        every { player.duration } returns C.TIME_UNSET
        every { player.bufferedPosition } returns 0L

        // When
        dataSource.seekTo(1000L)

        // Then
        dataSource.progress.test {
            val progress = awaitItem()
            assertEquals(episodeB.duration, progress.duration)
        }
    }

    // ---------------------------------------------------------------------
    // 4차 수정 회귀 테스트: 클립은 처음부터 클립 길이를 발행한다
    //
    // 클립은 ClippingConfiguration 으로 잘라 올리므로 실제로 흐르는 길이는 에피소드 전체가
    // 아니라 클립 길이다. 준비 전에 메타에서 길이를 가져올 때 전체 길이를 실으면, 클립 화면의
    // 남은 시간이 에피소드 전체 길이로 보였다가 progressUpdater 가 player.duration 을 읽는
    // 순간 클립 길이로 튄다.
    // ---------------------------------------------------------------------

    /** 전체 1시간짜리 에피소드에서 30초만 잘라낸 클립. 두 길이가 뚜렷이 달라야 폴백 오류가 드러난다. */
    private val clipEpisode = episodeTestData.copy(
        duration = 60.minutes,
        clipStartTime = Instant.fromEpochSeconds(100),
        clipDuration = 30.seconds,
    )

    /** media3 가 setMediaItem 호출 스택 안에서 transition 콜백을 인라인 실행하는 것을 흉내낸다. */
    private fun answerSetMediaItemWithInlineTransition() {
        every { player.setMediaItem(any()) } answers {
            listenerSlot.captured.onMediaItemTransition(
                firstArg<MediaItem>(),
                Player.MEDIA_ITEM_TRANSITION_REASON_PLAYLIST_CHANGED,
            )
        }
    }

    @Test
    fun `Given clip episode, When playClip fires inline transition, Then progress duration is the clip duration`() =
        runTest {
            // Given
            answerSetMediaItemWithInlineTransition()

            // When
            dataSource.playClip(clipEpisode)

            // Then: 첫 발행부터 클립 길이여야 한다. 여기서 전체 길이가 나오면 화면의 남은 시간이
            // 재생이 시작되는 순간 1시간에서 30초로 튄다.
            dataSource.progress.test {
                val progress = awaitItem()
                assertEquals(clipEpisode.clipDuration, progress.duration)
                assertEquals(clipEpisode.id, progress.episodeId)
            }
        }

    // --- 같은 클립을 두 번 올리지 않는다 ---
    //
    // setMediaItem 은 듣던 지점을 지운다. "같은 것을 다시 틀어 달라" 는 요청은 대개 화면이
    // 무엇이 올라가 있는지 잘못 알고 보낸 것이라, 그대로 따르면 사용자가 듣던 자리가 사라진다.
    // 화면이 기억으로 막으려 하면 그 기억이 컴포지션과 함께 사라지거나 실제와 어긋나므로,
    // 실제로 무엇이 올라가 있는지 아는 이 자리에서 막는다.

    /** 이미 그 클립이 올라가 재생 중인 상태를 만든다. */
    private fun givenClipIsLoadedAndPlaying(loaded: Episode) {
        val built = slot<MediaItem>()
        every { player.setMediaItem(capture(built)) } just Runs
        dataSource.playClip(loaded)
        every { player.currentMediaItem } returns built.captured
        every { player.playbackState } returns Player.STATE_READY
    }

    @Test
    fun `Given the same clip is already playing, When playClip called again, Then the listening position is left alone`() {
        // Given
        givenClipIsLoadedAndPlaying(clipEpisode)
        every { player.isPlaying } returns true
        clearMocks(player, answers = false, recordedCalls = true, verificationMarks = true)

        // When
        dataSource.playClip(clipEpisode)

        // Then: 다시 올리지 않는 것만으로는 부족하다. 되감거나 비우는 것도 듣던 자리를
        // 지우므로, 지점을 건드릴 수 있는 호출을 모두 막는다.
        verify(exactly = 0) { player.setMediaItem(any()) }
        verify(exactly = 0) { player.seekTo(any()) }
        verify(exactly = 0) { player.stop() }
        verify(exactly = 0) { player.clearMediaItems() }
    }

    @Test
    fun `Given the same clip is loaded but paused, When playClip called, Then it resumes without reloading`() {
        // 멈춰 둔 클립의 재생 버튼이 죽으면 안 된다 — 다시 올리지 않되 이어서 틀어야 한다.
        // Given
        givenClipIsLoadedAndPlaying(clipEpisode)
        every { player.isPlaying } returns false
        clearMocks(player, answers = false, recordedCalls = true, verificationMarks = true)

        // When
        dataSource.playClip(clipEpisode)

        // Then
        verify(exactly = 0) { player.setMediaItem(any()) }
        verify(exactly = 1) { player.play() }
    }

    @Test
    fun `Given the same episode with a different clip window, When playClip called, Then it is loaded again`() {
        // soundbites 는 episodeId 가 기본키라, 캐시가 갱신되면 같은 에피소드의 창만 바뀐
        // 행이 온다. 그때 넘기면 플레이어는 옛 창을, 카드는 새 길이를 말하게 된다.
        // Given
        givenClipIsLoadedAndPlaying(clipEpisode)
        clearMocks(player, answers = false, recordedCalls = true, verificationMarks = true)
        val shiftedWindow = clipEpisode.copy(clipStartTime = Instant.fromEpochSeconds(200))

        // When
        dataSource.playClip(shiftedWindow)

        // Then
        verify(exactly = 1) { player.setMediaItem(any()) }
    }

    @Test
    fun `Given a clipped item with no end position, When its duration is read, Then only what is left after the start counts`() =
        runTest {
            // toMediaItem 은 늘 양끝을 지정하지만, playbackDuration 은 "끝을 지정하지 않은
            // 클리핑" 도 답할 수 있어야 한다고 적어 두었다. 그 분기를 실제로 짚어 둔다.
            // Given
            val openEnded = MediaItem.Builder()
                .setMediaId(clipEpisode.id.toString())
                .setUri(mockk<Uri>(relaxed = true))
                .setTag(clipEpisode)
                .setClippingConfiguration(
                    MediaItem.ClippingConfiguration.Builder()
                        .setStartPositionMs(10.minutes.inWholeMilliseconds)
                        .build()
                )
                .build()
            every { player.currentMediaItem } returns openEnded
            every { player.duration } returns C.TIME_UNSET
            every { player.bufferedPosition } returns 0L

            // When
            dataSource.seekTo(0L)

            // Then: 60분 에피소드의 10분 지점부터면 남은 것은 50분이다.
            dataSource.progress.test {
                val progress = awaitItem()
                assertEquals(50.minutes, progress.duration)
            }
        }

    @Test
    fun `Given the clip ended, When playClip called with the same clip, Then it is loaded again`() {
        // 끝난 뒤 같은 클립을 다시 트는 것은 "처음부터 다시 듣기" 라는 정상 요청이다.
        // 지울 지점도 없으므로 막으면 안 된다.
        // Given
        givenClipIsLoadedAndPlaying(clipEpisode)
        every { player.playbackState } returns Player.STATE_ENDED
        clearMocks(player, answers = false, recordedCalls = true, verificationMarks = true)

        // When
        dataSource.playClip(clipEpisode)

        // Then
        verify(exactly = 1) { player.setMediaItem(any()) }
    }

    @Test
    fun `Given a different clip is playing, When playClip called, Then the new one is loaded`() {
        // Given
        givenClipIsLoadedAndPlaying(clipEpisode)
        clearMocks(player, answers = false, recordedCalls = true, verificationMarks = true)
        val other = episodeTestDataList[1].copy(
            clipStartTime = Instant.fromEpochSeconds(0),
            clipDuration = 15.seconds,
        )

        // When
        dataSource.playClip(other)

        // Then
        verify(exactly = 1) { player.setMediaItem(any()) }
    }

    @Test
    fun `Given a clip request for an episode without clip metadata, When repeated, Then it is not loaded again`() {
        // 잘라 올리지 않는 아이템도 "이미 올라 있다" 로 판정돼야 한다. 한때 판정 쪽만
        // clipEndPositionMs 와 견주는 바람에, 창 없이 올라간 아이템의 끝값(UNSET =
        // C.TIME_END_OF_SOURCE)과 영영 같아지지 않아 누를 때마다 처음으로 되감겼다.
        // Given
        val withoutClip = episodeTestData
        check(!withoutClip.hasClip) { "이 테스트는 클립 메타가 없는 데이터를 전제한다" }
        givenClipIsLoadedAndPlaying(withoutClip)
        every { player.isPlaying } returns true
        clearMocks(player, answers = false, recordedCalls = true, verificationMarks = true)

        // When
        dataSource.playClip(withoutClip)

        // Then
        verify(exactly = 0) { player.setMediaItem(any()) }
        verify(exactly = 0) { player.seekTo(any()) }
    }

    @Test
    fun `Given the loaded episode gains clip metadata, When playClip called, Then it is loaded again`() {
        // 같은 에피소드인데 hasClip 이 거짓에서 참으로 뒤집힌 경우. 창 없이 올라가 있던 것을
        // 그대로 두면 카드는 클립 길이를 보여주는데 플레이어는 에피소드 전체를 흘린다.
        // 판정이 두 상태를 가르지 못하면 이 어긋남이 조용히 남는다.
        // Given
        val withoutClip = episodeTestData
        check(!withoutClip.hasClip) { "이 테스트는 클립 메타가 없는 데이터를 전제한다" }
        givenClipIsLoadedAndPlaying(withoutClip)
        every { player.isPlaying } returns true
        clearMocks(player, answers = false, recordedCalls = true, verificationMarks = true)

        // When: 같은 에피소드에 클립 메타가 붙어 다시 들어온다
        val gainedClip = withoutClip.copy(
            clipStartTime = Instant.fromEpochSeconds(100),
            clipDuration = 30.seconds,
        )
        check(gainedClip.hasClip) { "뒤집힌 쪽은 클립이어야 한다" }
        dataSource.playClip(gainedClip)

        // Then
        verify(exactly = 1) { player.setMediaItem(any()) }
    }

    @Test
    fun `Given the player is idle, When playClip called with the loaded clip, Then it is loaded again`() {
        // 프로세스가 죽었다 살아난 자리. 태그는 남아 보여도 플레이어가 비어 있으면 올려야 한다.
        // Given
        givenClipIsLoadedAndPlaying(clipEpisode)
        every { player.playbackState } returns Player.STATE_IDLE
        clearMocks(player, answers = false, recordedCalls = true, verificationMarks = true)

        // When
        dataSource.playClip(clipEpisode)

        // Then
        verify(exactly = 1) { player.setMediaItem(any()) }
    }

    @Test
    fun `Given non-clip episode, When play fires inline transition, Then progress duration is the whole episode duration`() =
        runTest {
            // 클립 판정이 일반 재생까지 잡아채면 플레이어 화면의 길이가 무너진다.
            // 잘라 올리지 않은 아이템은 전체 길이 그대로여야 한다.
            answerSetMediaItemWithInlineTransition()

            // When
            dataSource.play(clipEpisode)

            // Then
            dataSource.progress.test {
                val progress = awaitItem()
                assertEquals(clipEpisode.duration, progress.duration)
            }
        }

    @Test
    fun `Given clip episodes, When playClips called, Then the confirmed emission carries the target's clip duration`() =
        runTest {
            // playClips 는 인라인 transition 을 isPreparing 으로 막고 확정값을 직접 발행한다.
            // 그 확정 발행도 클립 길이를 실어야 하고, 목록의 첫 항목이 아니라 재생할 항목의
            // 것이어야 한다 — indexToPlay 를 1 로 두어 둘을 구분한다.
            val target = episodeTestDataList[1].copy(
                duration = 90.minutes,
                clipStartTime = Instant.fromEpochSeconds(0),
                clipDuration = 15.seconds,
            )

            // When
            dataSource.playClips(episodes = listOf(clipEpisode, target), indexToPlay = 1)

            // Then
            dataSource.progress.test {
                val progress = awaitItem()
                assertEquals(target.clipDuration, progress.duration)
                assertEquals(target.id, progress.episodeId)
            }
        }

    @Test
    fun `Given clip episodes without clip metadata, When playClips called, Then progress duration falls back to the episode duration`() =
        runTest {
            // 클립 정보가 없으면 플레이어도 잘라 올리지 않는다. 그 경우 길이는 에피소드 전체다.
            val withoutClip = episodeTestData
            check(!withoutClip.hasClip) { "이 테스트는 클립 메타가 없는 데이터를 전제한다" }

            // When
            dataSource.playClips(episodes = listOf(withoutClip), indexToPlay = 0)

            // Then
            dataSource.progress.test {
                val progress = awaitItem()
                assertEquals(withoutClip.duration, progress.duration)
            }
        }

    @Test
    fun `Given a clipped media item with unset player duration, When seekTo called, Then progress duration falls back to the clip duration`() =
        runTest {
            // Given: 준비 전이라 player.duration 이 TIME_UNSET 이다. 이때 메타에서 길이를
            // 가져오는데, 잘라 올린 아이템이면 전체 길이가 아니라 클립 길이를 써야 한다.
            //
            // 아이템을 손으로 짓지 않고 playClip 이 실제로 만든 것을 되돌려 준다. 손으로 지으면
            // toMediaItem 에서 클리핑을 떼어내도 이 테스트는 초록으로 남는다 — 프로덕션이
            // 만들지 않는 표본을 검사하게 되기 때문이다.
            val built = slot<MediaItem>()
            every { player.setMediaItem(capture(built)) } just Runs
            dataSource.playClip(clipEpisode)

            every { player.currentMediaItem } returns built.captured
            every { player.duration } returns C.TIME_UNSET
            every { player.bufferedPosition } returns 0L

            // When
            dataSource.seekTo(1000L)

            // Then
            dataSource.progress.test {
                val progress = awaitItem()
                assertEquals(clipEpisode.clipDuration, progress.duration)
            }
        }

    @Test
    fun `Given a clip episode, When prepare called, Then progress duration is the whole episode duration because prepare does not clip`() =
        runTest {
            // prepare 도 메타에서 길이를 가져오는 네 지점 중 하나다. 지금은 늘 isClip = false 로
            // 올리므로 실제로는 전체 길이가 나오지만, 그건 toMediaItem 이 클리핑을 걸지 않기
            // 때문이지 prepare 가 예외라서가 아니다. 이 테스트는 판정 근거가 "경로" 가 아니라
            // "실제로 클리핑이 걸렸는가" 임을 고정한다.
            dataSource.prepare(listOf(clipEpisode), indexToPlay = 0, positionMs = 0L)

            dataSource.progress.test {
                val progress = awaitItem()
                assertEquals(clipEpisode.duration, progress.duration)
                assertEquals(clipEpisode.id, progress.episodeId)
            }
        }

    @Test
    fun `Given a spectrum monitor, When playback stops, Then the monitor is reset`() {
        // 일시정지 시 막대를 잠재우는 배선은 이 한 줄뿐인데, 지워도 전 모듈 테스트가 초록이었다.
        // 증상은 "정지했는데 막대 다섯이 마지막 모양 그대로 굳는다" 라 눈으로만 잡힌다.
        //
        // 재생 중 상태를 만들면 progressUpdater 가 Dispatchers.Main 을 찾는다. 이 파일은 Main 을
        // 세우지 않으므로(나머지 90개의 환경을 바꾸지 않으려는 것이다) 여기서만 잠깐 세워 둔다.
        Dispatchers.setMain(StandardTestDispatcher())
        val monitor = mockk<PlaybackSpectrumMonitor>(relaxed = true)
        val clipPlayer = mockk<ExoPlayer>(relaxed = true)
        val clipListener = slot<Player.Listener>()
        every { clipPlayer.addListener(capture(clipListener)) } just Runs
        val clipDataSource = PlayerDataSourceImpl(clipPlayer, episodeDownloader, monitor)

        try {
            clipListener.captured.onIsPlayingChanged(true)
            verify(exactly = 0) { monitor.reset() }

            clipListener.captured.onIsPlayingChanged(false)
            verify(exactly = 1) { monitor.reset() }
        } finally {
            clipDataSource.release()
            Dispatchers.resetMain()
        }
    }

    @Test
    fun `Given prepare with a restore position, When called, Then the position rides along with the duration`() =
        runTest {
            // prepare 를 publishStartOfPlayback 으로 합치면서 위치가 0 으로 뭉개지지 않는지 —
            // 이 경로의 존재 이유가 "앱을 다시 켰을 때 듣던 자리로 되돌리는 것" 이라 위치가
            // 사라지면 이어듣기가 통째로 망가진다.
            dataSource.prepare(listOf(episode), indexToPlay = 0, positionMs = 90_000L)

            dataSource.progress.test {
                val progress = awaitItem()
                assertEquals(90_000L.toDurationMillis(), progress.position)
                assertEquals(episode.duration, progress.duration)
                assertEquals(episode.id, progress.episodeId)
            }
        }
}
