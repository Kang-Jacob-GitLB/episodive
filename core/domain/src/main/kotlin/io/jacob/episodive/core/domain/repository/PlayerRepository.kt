package io.jacob.episodive.core.domain.repository

import androidx.media3.common.Player
import io.jacob.episodive.core.model.Episode
import io.jacob.episodive.core.model.Playback
import io.jacob.episodive.core.model.Progress
import io.jacob.episodive.core.model.Repeat
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

    val nowPlaying: Flow<Episode?>
    val playlist: Flow<List<Episode>>
    val indexOfList: Flow<Int>
    val progress: Flow<Progress>
    val playback: Flow<Playback>
    val isPlaying: Flow<Boolean>
    val isShuffle: Flow<Boolean>
    val repeat: Flow<Repeat>
    val speed: Flow<Float>
    val cue: Flow<String>
}
