package io.jacob.episodive.core.player.datasource

import androidx.media3.common.Player
import io.jacob.episodive.core.model.Episode
import io.jacob.episodive.core.model.PlaybackFailure
import io.jacob.episodive.core.model.Progress
import io.jacob.episodive.core.model.Spectrum
import io.jacob.episodive.core.model.Station
import io.jacob.episodive.core.model.StationStream
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

    /**
     * 플레이어에 지금 올라 있는 항목이 있는지. 에피소드가 아닌 항목(라이브 스트림 등)도 포함한다.
     * `nowPlaying` 은 에피소드만 싣기 때문에 null 이어도 플레이어가 비어 있다는 뜻은 아니다.
     * 플레이어를 직접 읽으므로 플레이어 스레드(메인)에서만 호출할 것.
     */
    fun hasMediaItem(): Boolean

    /**
     * 알림용 이니셜 아트를 IO 에서 미리 그려 캐시에 넣는다. [playStation] 이 캐시 히트로 메인 스레드에서
     * 비트맵을 만들지 않게 한다. 어느 스레드에서 불러도 된다.
     */
    suspend fun prepareStationArtwork(station: Station)

    /** 스테이션의 스트림을 올려 바로 튼다. 에피소드 재생과 같은 사용자 의도 경로다. */
    fun playStation(station: Station, stream: StationStream)

    /**
     * 지금 올라 있는 스테이션이 [stationId] 일 때만 같은(또는 [stream]) 항목을 다시 올린다. 했으면 true.
     * 재생 여부(playWhenReady)는 건드리지 않는다.
     */
    fun restartStation(stationId: String, stream: StationStream? = null): Boolean

    /** 지금 올라 있는 스테이션이 [stationId] 일 때만 멈추고 항목을 비운다. 했으면 true. */
    fun stopStation(stationId: String): Boolean

    /**
     * 사용자 의도로 현재 항목을 바꾼 호출 수. 복구가 "그 사이 사용자가 다른 걸 골랐는가" 를 가르는
     * 근거다. `prepare`(복원)·[restartStation] 은 세지 않는다. 플레이어 스레드(메인)에서만 호출할 것.
     */
    fun playRequestCount(): Long

    val nowPlaying: Flow<Episode?>

    /** 지금 올라 있는 스테이션. [nowPlaying] 과 배타다. */
    val nowPlayingStation: Flow<Station?>

    /** 스트림이 싣고 오는 "지금 나오는 곡" 정보(ICY/ID3). 항목이 바뀌면 null 로 돌아간다. */
    val streamTitle: Flow<String?>

    /** 재생 실패. 지난 실패를 다시 보내지 않는다(replay 없음). */
    val failures: Flow<PlaybackFailure>
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
