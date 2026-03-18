package io.jacob.episodive.core.player.datasource

import androidx.media3.common.Player
import io.jacob.episodive.core.model.Episode
import io.jacob.episodive.core.model.Progress
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
    fun addTrack(episode: Episode, index: Int? = null)
    fun addTrack(episodes: List<Episode>, index: Int? = null)
    fun addClipTrack(episode: Episode, index: Int? = null)
    fun addClipTracks(episodes: List<Episode>, index: Int? = null)
    fun removeTrack(index: Int)
    fun clearPlayList()
    fun release()

    val nowPlaying: Flow<Episode?>
    val playlist: Flow<List<Episode>>
    val indexOfList: Flow<Int>
    val progress: Flow<Progress>
    val playback: Flow<Int>
    val isPlaying: Flow<Boolean>
    val isShuffle: Flow<Boolean>
    val repeat: Flow<Int>
    val speed: Flow<Float>
    val cue: Flow<String>
}
