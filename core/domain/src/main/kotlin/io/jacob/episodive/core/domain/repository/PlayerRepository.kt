package io.jacob.episodive.core.domain.repository

import io.jacob.episodive.core.model.ClipEpisode
import io.jacob.episodive.core.model.Episode
import io.jacob.episodive.core.model.Playback
import io.jacob.episodive.core.model.Progress
import io.jacob.episodive.core.model.Repeat
import kotlinx.coroutines.flow.Flow

interface PlayerRepository {
    fun play(episode: Episode)
    fun play(episodes: List<Episode>, indexToPlay: Int? = null)
    fun playClip(clipEpisode: ClipEpisode)
    fun playClips(clipEpisodes: List<ClipEpisode>, indexToPlay: Int? = null)
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
    fun shuffle()
    fun changeRepeat()
    fun setSpeed(speed: Float)
    fun addTrack(episode: Episode, index: Int? = null)
    fun addTrack(episodes: List<Episode>, index: Int? = null)
    fun removeTrack(index: Int)
    fun clearPlayList()
    fun release()

    val nowPlaying: Flow<Episode?>
    val playlist: Flow<List<Episode>>
    val indexOfList: Flow<Int>
    val progress: Flow<Progress>
    val playback: Flow<Playback>
    val isPlaying: Flow<Boolean>
    val isShuffle: Flow<Boolean>
    val repeat: Flow<Repeat>
    val speed: Flow<Float>
}