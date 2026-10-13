package io.jacob.episodive.core.domain.repository

import androidx.media3.common.Player
import io.jacob.episodive.core.model.Episode
import io.jacob.episodive.core.model.PlaybackFailure
import io.jacob.episodive.core.model.Playback
import io.jacob.episodive.core.model.Progress
import io.jacob.episodive.core.model.Repeat
import io.jacob.episodive.core.model.Spectrum
import io.jacob.episodive.core.model.Station
import io.jacob.episodive.core.model.StationStream
import kotlinx.coroutines.flow.Flow

interface PlayerRepository {
    fun getPlayer(): Player
    fun play(episode: Episode)
    fun play(episodes: List<Episode>, indexToPlay: Int? = null)
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
    fun prepare(episodes: List<Episode>, indexToPlay: Int, positionMs: Long)
    fun shuffle()
    fun setShuffle(isShuffle: Boolean)
    fun changeRepeat()
    fun setRepeat(repeat: Repeat)
    fun setSpeed(speed: Float)
    fun setVolume(volume: Float)
    fun addTrack(episode: Episode, index: Int? = null)
    fun addTrack(episodes: List<Episode>, index: Int? = null)
    fun addClipTrack(episode: Episode, index: Int? = null)
    fun addClipTracks(episodes: List<Episode>, index: Int? = null)
    fun removeTrack(index: Int)
    fun clearPlayList()
    fun release()

    /**
     * process 재시작 직후 ExoPlayer 가 이전 세션을 이어 재생하지만 `_nowPlaying`
     * StateFlow 는 null 인 상태에서, 마지막 재생 Episode 로 1회 동기화한다.
     * `play(episode)` 와 달리 player 큐/재생 상태를 변경하지 않고 메타데이터만 hydrate.
     */
    fun rehydrate(episode: Episode)

    /**
     * 플레이어에 지금 올라 있는 항목이 있는지. 에피소드가 아닌 항목(라이브 스트림 등)도 포함한다.
     * [nowPlaying] 은 에피소드만 싣기 때문에 null 이어도 플레이어가 비어 있다는 뜻은 아니다.
     * 플레이어를 직접 읽으므로 플레이어 스레드(메인)에서만 호출할 것.
     */
    fun hasMediaItem(): Boolean

    /** 알림용 아트를 미리 그려 둔다. [playStation] 이 메인에서 그리지 않게 한다. 어느 스레드에서 불러도 된다. */
    suspend fun prepareStationArtwork(station: Station)

    /** 스테이션의 스트림을 올려 바로 튼다. */
    fun playStation(station: Station, stream: StationStream)

    /**
     * 지금 올라 있는 스테이션이 [stationId] 일 때만 같은(또는 [stream]) 항목을 다시 올린다. 했으면 true.
     * 재생 여부는 건드리지 않는다.
     */
    fun restartStation(stationId: String, stream: StationStream? = null): Boolean

    /** 지금 올라 있는 스테이션이 [stationId] 일 때만 멈추고 항목을 비운다. 했으면 true. */
    fun stopStation(stationId: String): Boolean

    /**
     * 사용자 의도로 현재 항목을 바꾼 호출 수. 복원·[restartStation] 은 세지 않는다.
     * 플레이어 스레드(메인)에서만 호출할 것.
     */
    fun playRequestCount(): Long

    val nowPlaying: Flow<Episode?>

    /** 지금 올라 있는 스테이션. [nowPlaying] 과 배타이며, 둘을 combine 해 판별하지 말 것. */
    val nowPlayingStation: Flow<Station?>

    /** 라이브로 판명된 현재 에피소드의 id. 판명 전이거나 라이브가 아니면 null. */
    val liveEpisodeId: Flow<Long?>

    /** 스트림이 싣고 오는 "지금 나오는 곡" 정보. 스테이션이 아니거나 항목이 바뀌면 null. */
    val streamTitle: Flow<String?>

    /** 재생 실패. 지난 실패를 다시 보내지 않는다(replay 없음). */
    val failures: Flow<PlaybackFailure>
    val playlist: Flow<List<Episode>>
    val indexOfList: Flow<Int>
    val progress: Flow<Progress>
    val playback: Flow<Playback>
    val isPlaying: Flow<Boolean>

    /** 재생을 요청했지만 아직 준비(버퍼링) 중이라 소리가 나지 않는 동안 참. 멈춘 채 버퍼링하면 거짓. */
    val isBuffering: Flow<Boolean>
    val isShuffle: Flow<Boolean>
    val repeat: Flow<Repeat>
    val speed: Flow<Float>
    val cue: Flow<String>

    /**
     * 탐색(시크)이 일어난 순간을 알리는 신호. VTT 자막(`translatedCues`)이 시크로 롤링 history 를
     * 비우는 데 쓴다 — 에피소드 구분과 마찬가지로 `progress.episodeId` 가 아니라 이 신호 자체가
     * "구간이 바뀌었다" 는 근거다.
     */
    val seeks: Flow<Unit>

    /**
     * 지금 나고 있는 소리를 주파수 대역 다섯 칸으로 나눈 세기(각 0..1). 분석을 붙이지 않은
     * 플레이어는 늘 잠잠하다. 실제로 귀에 닿는 소리보다 AudioTrack 버퍼만큼 앞선 값이다.
     */
    val spectrum: Flow<Spectrum>
}
