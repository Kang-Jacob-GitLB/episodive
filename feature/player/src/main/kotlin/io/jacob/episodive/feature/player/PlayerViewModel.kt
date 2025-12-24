package io.jacob.episodive.feature.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.jacob.episodive.core.common.EpisodivePlayers
import io.jacob.episodive.core.common.Player
import io.jacob.episodive.core.common.combine
import io.jacob.episodive.core.domain.repository.PlayerRepository
import io.jacob.episodive.core.domain.usecase.episode.GetChaptersUseCase
import io.jacob.episodive.core.domain.usecase.episode.IsLikedUseCase
import io.jacob.episodive.core.domain.usecase.episode.ToggleLikedUseCase
import io.jacob.episodive.core.domain.usecase.episode.UpdatePlayedEpisodeUseCase
import io.jacob.episodive.core.domain.usecase.player.GetPlaylistUseCase
import io.jacob.episodive.core.domain.usecase.podcast.GetPodcastUseCase
import io.jacob.episodive.core.domain.usecase.podcast.ToggleFollowedUseCase
import io.jacob.episodive.core.domain.usecase.user.GetUserDataUseCase
import io.jacob.episodive.core.domain.usecase.user.SetSpeedUseCase
import io.jacob.episodive.core.model.Chapter
import io.jacob.episodive.core.model.Episode
import io.jacob.episodive.core.model.Podcast
import io.jacob.episodive.core.model.Progress
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val isLikedUseCase: IsLikedUseCase,
    private val toggleLikedUseCase: ToggleLikedUseCase,
    private val updatePlayedEpisodeUseCase: UpdatePlayedEpisodeUseCase,
    private val getPodcastUseCase: GetPodcastUseCase,
    @param:Player(EpisodivePlayers.Main) private val playerRepository: PlayerRepository,
    getPlaylistUseCase: GetPlaylistUseCase,
    private val setSpeedUseCase: SetSpeedUseCase,
    private val getUserDataUseCase: GetUserDataUseCase,
    private val getChaptersUseCase: GetChaptersUseCase,
    private val toggleFollowedUseCase: ToggleFollowedUseCase,
) : ViewModel() {
    private val playingEpisode = combine(
        playerRepository.nowPlaying,
        playerRepository.progress,
    ) { episode, progress ->
        episode?.id to progress
    }

    private val podcast = playerRepository.nowPlaying.mapNotNull { it?.feedId }
        .flatMapLatest { feedId -> getPodcastUseCase(feedId) }

    private val isLiked = playerRepository.nowPlaying.mapNotNull { it?.id }
        .flatMapLatest { episodeId -> isLikedUseCase(episodeId) }

    private val chapters = playerRepository.nowPlaying.map { it?.chaptersUrl }
        .flatMapLatest { chaptersUrl ->
            val chapters = chaptersUrl?.let { getChaptersUseCase(it) } ?: emptyList()
            flowOf(chapters)
        }


    val state: StateFlow<PlayerState> = combine(
        podcast,
        playerRepository.nowPlaying,
        getPlaylistUseCase(),
        playerRepository.indexOfList,
        playerRepository.progress,
        playerRepository.isPlaying,
        playerRepository.speed,
        isLiked,
        chapters,
        playerRepository.cue,
    ) { podcast, nowPlaying, playlist, indexOfList, progress, isPlaying, speed, isLiked, chapters, cue ->
        if (podcast != null && nowPlaying != null) {
            PlayerState.Success(
                podcast = podcast,
                nowPlaying = nowPlaying,
                playlist = playlist,
                indexOfList = indexOfList,
                progress = progress,
                isPlaying = isPlaying,
                speed = speed,
                isLiked = isLiked,
                chapters = chapters,
                cue = cue,
            ) as PlayerState
        } else {
            PlayerState.Error("podcast or nowPlaying is null")
        }
    }.catch { e ->
        emit(PlayerState.Error(e.message ?: "Unknown error"))
        e.printStackTrace()
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = PlayerState.Loading
    )

    private val _action = MutableSharedFlow<PlayerAction>(extraBufferCapacity = 1)

    private val _effect = MutableSharedFlow<PlayerEffect>(extraBufferCapacity = 1)
    val effect = _effect.asSharedFlow()

    init {
        handleActions()
        viewModelScope.launch {
            playingEpisode.collectLatest { (episodeId, progress) ->
                if (episodeId != null) {
                    updatePlayedEpisodeUseCase(episodeId, progress)
                }
            }
        }
        viewModelScope.launch {
            getUserDataUseCase()
                .mapNotNull { it.speed }
                .distinctUntilChanged()
                .first()
                .let { speed ->
                    playerRepository.setSpeed(speed)
                }
        }
    }

    private fun handleActions() = viewModelScope.launch {
        _action.collectLatest { action ->
            when (action) {
                is PlayerAction.PlayOrPause -> playOrPause()
                is PlayerAction.Next -> next()
                is PlayerAction.Previous -> previous()
                is PlayerAction.Shuffle -> shuffle()
                is PlayerAction.Repeat -> repeat()
                is PlayerAction.PlayIndex -> playIndex(action.index)
                is PlayerAction.SeekTo -> seekTo(action.position)
                is PlayerAction.SeekBackward -> seekBackward()
                is PlayerAction.SeekForward -> seekForward()
                is PlayerAction.Speed -> speed(action.speed)
                is PlayerAction.ClickPodcast -> clickPodcast(action.podcast)
                is PlayerAction.ClickEpisode -> clickEpisode(action.episode)
                is PlayerAction.ToggleLike -> toggleCurrentEpisodeLiked()
                is PlayerAction.ToggleEpisodeLiked -> toggleEpisodeLiked(action.episode)
                is PlayerAction.TogglePodcastFollowed -> togglePodcastFollowed(action.podcast)
                is PlayerAction.ExpandPlayer -> expandPlayer()
                is PlayerAction.CollapsePlayer -> collapsePlayer()
            }
        }
    }

    fun sendAction(action: PlayerAction) = viewModelScope.launch {
        _action.emit(action)
    }

    private fun playOrPause() = viewModelScope.launch {
        playerRepository.playOrPause()
    }

    private fun next() {
        playerRepository.next()
    }

    private fun previous() {
        playerRepository.previous()
    }

    private fun shuffle() {
        playerRepository.shuffle()
    }

    private fun repeat() {
        playerRepository.changeRepeat()
    }

    private fun playIndex(index: Int) {
        playerRepository.playIndex(index)
    }

    private fun seekTo(position: Long) {
        playerRepository.seekTo(position)
    }

    private fun seekBackward() {
        playerRepository.seekBackward()
    }

    private fun seekForward() {
        playerRepository.seekForward()
    }

    private fun speed(speed: Float) = viewModelScope.launch {
        playerRepository.setSpeed(speed)
        setSpeedUseCase(speed)
    }

    private fun clickPodcast(podcast: Podcast) = viewModelScope.launch {
        _effect.emit(PlayerEffect.NavigateToPodcast(podcast))
    }

    private fun clickEpisode(episode: Episode) = viewModelScope.launch {
        val currentState = state.value
        if (currentState is PlayerState.Success) {
            val index = currentState.playlist.indexOf(episode)
            playerRepository.playIndex(index)
        }
    }

    private fun toggleCurrentEpisodeLiked() = viewModelScope.launch {
        val currentState = state.value
        if (currentState is PlayerState.Success) {
            toggleLikedUseCase(currentState.nowPlaying.id)
        }
    }

    private fun toggleEpisodeLiked(episode: Episode) = viewModelScope.launch {
        toggleLikedUseCase(episode.id)
    }

    private fun togglePodcastFollowed(podcast: Podcast) = viewModelScope.launch {
        toggleFollowedUseCase(podcast.id)
    }

    private fun expandPlayer() = viewModelScope.launch {
        _effect.emit(PlayerEffect.ShowPlayerBottomSheet)
    }

    private fun collapsePlayer() = viewModelScope.launch {
        _effect.emit(PlayerEffect.HidePlayerBottomSheet)
    }
}

sealed interface PlayerState {
    data object Loading : PlayerState
    data class Success(
        val podcast: Podcast,
        val nowPlaying: Episode,
        val playlist: List<Episode>,
        val indexOfList: Int,
        val progress: Progress,
        val isPlaying: Boolean,
        val speed: Float,
        val isLiked: Boolean,
        val chapters: List<Chapter>,
        val cue: String,
    ) : PlayerState

    data class Error(val message: String) : PlayerState
}

sealed interface PlayerAction {
    data object PlayOrPause : PlayerAction
    data object Next : PlayerAction
    data object Previous : PlayerAction
    data object Shuffle : PlayerAction
    data object Repeat : PlayerAction
    data class PlayIndex(val index: Int) : PlayerAction
    data class SeekTo(val position: Long) : PlayerAction
    data object SeekBackward : PlayerAction
    data object SeekForward : PlayerAction
    data class Speed(val speed: Float) : PlayerAction
    data class ClickPodcast(val podcast: Podcast) : PlayerAction
    data class ClickEpisode(val episode: Episode) : PlayerAction
    data object ToggleLike : PlayerAction
    data class ToggleEpisodeLiked(val episode: Episode) : PlayerAction
    data class TogglePodcastFollowed(val podcast: Podcast) : PlayerAction
    data object ExpandPlayer : PlayerAction
    data object CollapsePlayer : PlayerAction
}

sealed interface PlayerEffect {
    data class NavigateToPodcast(val podcast: Podcast) : PlayerEffect
    data object ShowPlayerBottomSheet : PlayerEffect
    data object HidePlayerBottomSheet : PlayerEffect
}