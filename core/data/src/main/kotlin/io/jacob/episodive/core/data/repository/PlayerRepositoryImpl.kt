package io.jacob.episodive.core.data.repository

import io.jacob.episodive.core.domain.repository.PlayerRepository
import io.jacob.episodive.core.model.Episode
import io.jacob.episodive.core.model.Playback
import io.jacob.episodive.core.model.Progress
import io.jacob.episodive.core.model.Repeat
import io.jacob.episodive.core.player.datasource.PlayerDataSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class PlayerRepositoryImpl @Inject constructor(
    private val playerDataSource: PlayerDataSource
) : PlayerRepository {
    override fun play(episode: Episode) {
        playerDataSource.play(episode)
    }

    override fun play(episodes: List<Episode>, indexToPlay: Int?) {
        playerDataSource.play(episodes, indexToPlay)
    }

    override fun playClip(episode: Episode) {
        playerDataSource.playClip(episode)
    }

    override fun playClips(episodes: List<Episode>, indexToPlay: Int?) {
        playerDataSource.playClips(episodes, indexToPlay)
    }

    override fun playIndex(index: Int) {
        playerDataSource.playIndex(index)
    }

    override fun playOrPause() {
        playerDataSource.playOrPause()
    }

    override fun pause() {
        playerDataSource.pause()
    }

    override fun resume() {
        playerDataSource.resume()
    }

    override fun stop() {
        playerDataSource.stop()
    }

    override fun next() {
        playerDataSource.next()
    }

    override fun previous() {
        playerDataSource.previous()
    }

    override fun seekTo(position: Long) {
        playerDataSource.seekTo(position)
    }

    override fun seekBackward() {
        playerDataSource.seekBackward()
    }

    override fun seekForward() {
        playerDataSource.seekForward()
    }

    override fun shuffle() {
        playerDataSource.shuffle()
    }

    override fun changeRepeat() {
        playerDataSource.changeRepeat()
    }

    override fun setSpeed(speed: Float) {
        playerDataSource.setSpeed(speed)
    }

    override fun addTrack(episode: Episode, index: Int?) {
        playerDataSource.addTrack(episode, index)
    }

    override fun addTrack(episodes: List<Episode>, index: Int?) {
        playerDataSource.addTrack(episodes, index)
    }

    override fun addClipTrack(episode: Episode, index: Int?) {
        playerDataSource.addClipTrack(episode, index)
    }

    override fun addClipTracks(episodes: List<Episode>, index: Int?) {
        playerDataSource.addClipTracks(episodes, index)
    }

    override fun removeTrack(index: Int) {
        playerDataSource.removeTrack(index)
    }

    override fun clearPlayList() {
        playerDataSource.clearPlayList()
    }

    override fun release() {
        playerDataSource.release()
    }

    override val nowPlaying: Flow<Episode?> = playerDataSource.nowPlaying
    override val playlist: Flow<List<Episode>> = playerDataSource.playlist
    override val indexOfList: Flow<Int> = playerDataSource.indexOfList
    override val progress: Flow<Progress> = playerDataSource.progress
    override val playback: Flow<Playback> = playerDataSource.playback.map { Playback.fromValue(it) }
    override val isPlaying: Flow<Boolean> = playerDataSource.isPlaying
    override val isShuffle: Flow<Boolean> = playerDataSource.isShuffle
    override val repeat: Flow<Repeat> = playerDataSource.repeat.map { Repeat.fromValue(it) }
    override val speed: Flow<Float> = playerDataSource.speed
}