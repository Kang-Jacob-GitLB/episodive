package io.jacob.episodive.core.player.datasource

import androidx.media3.common.Player
import io.jacob.episodive.core.model.Episode
import io.jacob.episodive.core.model.Progress
import io.jacob.episodive.core.model.Spectrum
import kotlinx.coroutines.flow.Flow

interface PlayerDataSource {
    fun getPlayer(): Player
    fun play(episode: Episode)
    fun play(episodes: List<Episode>, indexToPlay: Int? = null)
    fun prepare(episodes: List<Episode>, indexToPlay: Int, positionMs: Long)
    fun playClip(episode: Episode)
    fun playClips(episodes: List<Episode>, indexToPlay: Int? = null)
    fun playIndex(index: Int)
    fun playOrPause()
    fun pause()
    fun resume()
    fun stop()
    fun next()
    fun previous()
    fun seekTo(position: Long)
    fun seekBackward()
    fun seekForward()
    fun setShuffle(isShuffle: Boolean)
    fun shuffle()
    fun setRepeat(repeat: Int)
    fun changeRepeat()
    fun setSpeed(speed: Float)
    fun setVolume(volume: Float)
    fun addTrack(episode: Episode, index: Int? = null)
    fun addTrack(episodes: List<Episode>, index: Int? = null)
    fun addClipTrack(episode: Episode, index: Int? = null)
    fun addClipTracks(episodes: List<Episode>, index: Int? = null)
    fun removeTrack(index: Int)
    fun clearPlayList()
    fun release()

    /** _nowPlaying / _isPlaying 을 외부 source 로 1회 동기화 (process restart hydration 용). */
    fun rehydrate(episode: Episode)

    val nowPlaying: Flow<Episode?>
    val playlist: Flow<List<Episode>>
    val indexOfList: Flow<Int>
    val progress: Flow<Progress>
    val playback: Flow<Int>
    val isPlaying: Flow<Boolean>

    /** 재생을 요청했지만 아직 준비(버퍼링) 중이라 소리가 나지 않는 동안 참. 멈춘 채 버퍼링하면 거짓. */
    val isBuffering: Flow<Boolean>
    val isShuffle: Flow<Boolean>
    val repeat: Flow<Int>
    val speed: Flow<Float>
    val cue: Flow<String>

    /**
     * 탐색(시크)이 일어난 순간을 알리는 신호. VTT 경로가 이 신호로 롤링 자막 history 를 비운다 —
     * cue 는 빈 문자열로 온 뒤에도 화면이 지난 줄을 계속 쌓아 두므로, 시크를 따로 알리지 않으면
     * 지난 구간의 자막이 새 위치 아래에 그대로 남는다.
     */
    val seeks: Flow<Unit>

    /**
     * 지금 나고 있는 소리를 주파수 대역 다섯 칸으로 나눈 세기(각 0..1). 분석을 붙이지 않은
     * 플레이어는 늘 잠잠하다. 실제로 귀에 닿는 소리보다 AudioTrack 버퍼만큼 앞선 값이다.
     */
    val spectrum: Flow<Spectrum>
}
