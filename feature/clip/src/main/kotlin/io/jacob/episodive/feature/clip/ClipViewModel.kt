package io.jacob.episodive.feature.clip

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.cachedIn
import dagger.hilt.android.lifecycle.HiltViewModel
import io.jacob.episodive.core.common.EpisodivePlayers
import io.jacob.episodive.core.common.Player
import io.jacob.episodive.core.domain.repository.PlayerRepository
import io.jacob.episodive.core.domain.usecase.episode.GetClipEpisodesPagingUseCase
import io.jacob.episodive.core.domain.usecase.episode.ToggleLikedEpisodeUseCase
import io.jacob.episodive.core.domain.usecase.player.PlayEpisodeUseCase
import io.jacob.episodive.core.model.Episode
import io.jacob.episodive.core.model.Playback
import io.jacob.episodive.core.model.Progress
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds

@HiltViewModel
class ClipViewModel @Inject constructor(
    getClipEpisodesPagingUseCase: GetClipEpisodesPagingUseCase,
    @param:Player(EpisodivePlayers.Clip) private val playerRepository: PlayerRepository,
    private val playEpisodeUseCase: PlayEpisodeUseCase,
    private val toggleLikedEpisodeUseCase: ToggleLikedEpisodeUseCase,
) : ViewModel() {
    val episodes = getClipEpisodesPagingUseCase(100).cachedIn(viewModelScope)

    val clipPlayerState: StateFlow<ClipPlayerState> = combine(
        playerRepository.playback,
        playerRepository.progress,
        playerRepository.isPlaying,
    ) { playback, progress, isPlaying ->
        ClipPlayerState(
            playback = playback,
            progress = progress,
            isPlaying = isPlaying,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ClipPlayerState()
    )

    private val _action = MutableSharedFlow<ClipAction>(extraBufferCapacity = 1)

    private val _effect = MutableSharedFlow<ClipEffect>(extraBufferCapacity = 1)
    val effect = _effect.asSharedFlow()

    init {
        handleActions()
    }

    private fun handleActions() = viewModelScope.launch {
        _action.collectLatest { action ->
            when (action) {
                is ClipAction.PlayClip -> playClip(action.episode)
                is ClipAction.ClickEpisode -> playEpisode(action.episode)
                is ClipAction.ToggleLikedEpisode -> toggleLikedEpisode(action.episode)
                is ClipAction.ClickPodcast -> clickPodcast(action.podcastId)
                is ClipAction.Resume -> resume()
                is ClipAction.Pause -> pause()
            }
        }
    }

    fun sendAction(action: ClipAction) = viewModelScope.launch {
        _action.emit(action)
    }

    private fun playClip(episode: Episode) {
        playerRepository.playClip(episode)
    }

    private fun resume() {
        playerRepository.resume()
    }

    private fun pause() {
        playerRepository.pause()
    }

    private fun playEpisode(episode: Episode) = viewModelScope.launch {
        playEpisodeUseCase(episode)
    }

    private fun toggleLikedEpisode(episode: Episode) = viewModelScope.launch {
        toggleLikedEpisodeUseCase(episode)
    }


    private fun clickPodcast(podcastId: Long) = viewModelScope.launch {
        _effect.emit(ClipEffect.NavigateToPodcast(podcastId))
    }
}

data class ClipPlayerState(
    val playback: Playback = Playback.IDLE,
    val progress: Progress = Progress(0.seconds, 0.seconds, 0.seconds),
    val isPlaying: Boolean = false,
)

sealed interface ClipAction {
    data class PlayClip(val episode: Episode) : ClipAction
    data class ClickEpisode(val episode: Episode) : ClipAction
    data class ToggleLikedEpisode(val episode: Episode) : ClipAction
    data class ClickPodcast(val podcastId: Long) : ClipAction
    data object Resume : ClipAction
    data object Pause : ClipAction
}

sealed interface ClipEffect {
    data class NavigateToPodcast(val podcastId: Long) : ClipEffect
}