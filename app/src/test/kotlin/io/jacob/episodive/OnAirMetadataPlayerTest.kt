package io.jacob.episodive

import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import io.jacob.episodive.core.testing.model.kbsFirstRadioStationTestData
import io.jacob.episodive.core.testing.model.radioStationTestData
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class OnAirMetadataPlayerTest {
    private val station = radioStationTestData
    private val imageUri = Uri.parse("https://img.example.com/program.jpg")
    private val initialsArt = byteArrayOf(1, 2, 3)

    // 스테이션 항목이 싣는 원본 메타데이터: 이름·방송사·이니셜 아트(Station.toMediaItem 과 같다).
    private val rawStationMetadata = MediaMetadata.Builder()
        .setTitle(station.name)
        .setDisplayTitle(station.name)
        .setArtist(station.broadcaster)
        .setArtworkData(initialsArt, MediaMetadata.PICTURE_TYPE_FRONT_COVER)
        .build()

    private fun stationItem(s: io.jacob.episodive.core.model.Station = station) =
        MediaItem.Builder().setMediaId("station:${s.id}").setUri("https://live.example.com/a.m3u8").setTag(s).build()

    private val episodeItem = MediaItem.Builder().setMediaId("42").setUri("https://ep.example.com/a.mp3").build()

    private val player = mockk<Player>(relaxed = true)
    private val addedListener = slot<Player.Listener>()

    private fun wrapper(current: MediaItem? = stationItem(), raw: MediaMetadata = rawStationMetadata): OnAirMetadataPlayer {
        every { player.mediaMetadata } returns raw
        every { player.currentMediaItem } returns current
        every { player.addListener(capture(addedListener)) } returns Unit
        return OnAirMetadataPlayer(player)
    }

    private fun overlay(artwork: Uri? = imageUri, artist: String = "아침 라디오 · 홍길동") =
        OnAirMetadataPlayer.Overlay(station = station, artist = artist, artworkUri = artwork)

    private class Recorder : Player.Listener {
        val metadata = mutableListOf<MediaMetadata>()
        val events = mutableListOf<Player.Events>()
        var isPlayingChanges = mutableListOf<Boolean>()

        override fun onMediaMetadataChanged(mediaMetadata: MediaMetadata) {
            metadata += mediaMetadata
        }

        override fun onEvents(player: Player, events: Player.Events) {
            this.events += events
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            isPlayingChanges += isPlaying
        }
    }

    @Test
    fun `Given 덧씌움 없음, Then 원본 메타데이터 그대로다`() {
        assertEquals(rawStationMetadata, wrapper().mediaMetadata)
    }

    @Test
    fun `Given 스테이션 재생 중에 프로그램을 앎, Then 제목은 스테이션 이름 본문은 프로그램이고 아트는 uri 로 바뀐다`() {
        val w = wrapper()
        w.setOverlay(overlay())

        val m = w.mediaMetadata
        assertEquals(station.name, m.title)
        assertEquals(station.name, m.displayTitle)
        assertEquals("아침 라디오 · 홍길동", m.artist)
        assertEquals(imageUri, m.artworkUri)
        // artworkData 가 남아 있으면 세션의 BitmapLoader 가 uri 를 무시한다.
        assertNull(m.artworkData)
    }

    @Test
    fun `Given 프로그램 이미지가 없음, Then 제목과 본문은 덧씌우고 원래 이니셜 아트를 유지한다`() {
        val w = wrapper()
        w.setOverlay(overlay(artwork = null))

        val m = w.mediaMetadata
        assertEquals("아침 라디오 · 홍길동", m.artist)
        assertArrayEquals(initialsArt, m.artworkData)
        assertNull(m.artworkUri)
    }

    @Test
    fun `Given 에피소드가 올라 있음, Then 덧씌움이 있어도 원본 그대로다`() {
        val raw = MediaMetadata.Builder().setTitle("에피소드").setArtist("팟캐스트").build()
        val w = wrapper(current = episodeItem, raw = raw)
        w.setOverlay(overlay())

        assertEquals(raw, w.mediaMetadata)
    }

    @Test
    fun `Given 올라 있는 항목이 다른 스테이션, Then 이전 스테이션의 덧씌움은 적용되지 않는다`() {
        val w = wrapper(current = stationItem(kbsFirstRadioStationTestData))
        w.setOverlay(overlay())

        assertEquals(rawStationMetadata, w.mediaMetadata)
    }

    @Test
    fun `Given 항목이 없음, Then 원본 그대로다`() {
        val w = wrapper(current = null)
        w.setOverlay(overlay())

        assertEquals(rawStationMetadata, w.mediaMetadata)
    }

    @Test
    fun `Given 리스너, When 덧씌움이 바뀜, Then 메타데이터 변경과 이벤트를 알리고 같은 값이면 알리지 않는다`() {
        val w = wrapper()
        val listener = Recorder()
        w.addListener(listener)

        w.setOverlay(overlay())
        assertEquals(1, listener.metadata.size)
        assertEquals("아침 라디오 · 홍길동", listener.metadata.single().artist)
        assertTrue(listener.events.single().contains(Player.EVENT_MEDIA_METADATA_CHANGED))

        w.setOverlay(overlay())
        assertEquals(1, listener.metadata.size)

        w.setOverlay(overlay(artist = "점심 라디오"))
        assertEquals("점심 라디오", listener.metadata.last().artist)
        assertEquals(2, listener.metadata.size)

        // 프로그램을 모르게 되면 원본으로 돌아간다.
        w.setOverlay(null)
        assertEquals(rawStationMetadata, listener.metadata.last())
    }

    @Test
    fun `Given 플레이어가 원본 메타데이터를 알림, Then 리스너는 덧씌운 값을 받는다`() {
        val w = wrapper()
        w.setOverlay(overlay())
        val listener = Recorder()
        w.addListener(listener)

        // 스트림 메타데이터가 올 때마다 플레이어가 원본을 내보낸다. 세션이 이 인자를 playerInfo 에 넣는다.
        addedListener.captured.onMediaMetadataChanged(rawStationMetadata)

        assertEquals("아침 라디오 · 홍길동", listener.metadata.single().artist)
        assertEquals(imageUri, listener.metadata.single().artworkUri)
    }

    @Test
    fun `Given 에피소드, When 플레이어가 메타데이터를 알림, Then 원본 인자 그대로 전달한다`() {
        val raw = MediaMetadata.Builder().setTitle("에피소드").build()
        val w = wrapper(current = episodeItem, raw = raw)
        val listener = Recorder()
        w.addListener(listener)

        addedListener.captured.onMediaMetadataChanged(raw)

        assertEquals(listOf(raw), listener.metadata)
    }

    @Test
    fun `Given 감싼 리스너, Then 메타데이터 외 콜백도 그대로 위임한다`() {
        val w = wrapper()
        val listener = Recorder()
        w.addListener(listener)

        addedListener.captured.onIsPlayingChanged(true)

        assertEquals(listOf(true), listener.isPlayingChanges)
    }

    @Test
    fun `Given 리스너를 제거, Then 같은 감싼 리스너로 플레이어에서 뺀다`() {
        val w = wrapper()
        val listener = Recorder()
        w.addListener(listener)
        val registered = addedListener.captured

        w.removeListener(listener)
        verify(exactly = 1) { player.removeListener(registered) }

        // 제거된 리스너에는 더는 알리지 않는다.
        w.setOverlay(overlay())
        assertTrue(listener.metadata.isEmpty())
    }

    @Test
    fun `Given 이벤트, Then 메타데이터 변경만 담긴다`() {
        val w = wrapper()
        val listener = Recorder()
        w.addListener(listener)

        w.setOverlay(overlay())

        val events = listener.events.single()
        assertEquals(1, events.size())
        assertEquals(Player.EVENT_MEDIA_METADATA_CHANGED, events.get(0))
    }

    @Test
    fun `Given Player Listener 의 모든 콜백, Then 감싼 리스너가 전부 직접 위임한다`() {
        // 자바 default 메서드는 코틀린 위임이 넘겨 주지 않는다. media3 를 올려 콜백이 늘면 여기서 빠진 것을 알린다.
        val wrapperClass = Class.forName("io.jacob.episodive.OnAirMetadataPlayer\$OverlayListener")
        val declared = wrapperClass.declaredMethods.map { it.name to it.parameterTypes.toList() }.toSet()

        val missing = Player.Listener::class.java.methods
            .filter { it.declaringClass == Player.Listener::class.java }
            .filterNot { (it.name to it.parameterTypes.toList()) in declared }
            .map { "${it.name}(${it.parameterTypes.joinToString { type -> type.simpleName }})" }

        assertTrue("위임하지 않는 Player.Listener 콜백: $missing", missing.isEmpty())
    }
}
