package io.jacob.episodive.feature.clip

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.jacob.episodive.core.domain.di.ClipPlayerRepository
import io.jacob.episodive.core.domain.repository.PlayerRepository
import io.jacob.episodive.core.domain.usecase.episode.GetClipEpisodesUseCase
import io.jacob.episodive.core.domain.usecase.episode.ToggleLikedUseCase
import io.jacob.episodive.core.domain.usecase.player.PlayAndAddClipsUseCase
import io.jacob.episodive.core.domain.usecase.player.PlayEpisodeUseCase
import io.jacob.episodive.core.model.Episode
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

@HiltViewModel
class ClipViewModel @Inject constructor(
    getClipEpisodesUseCase: GetClipEpisodesUseCase,
    @param:ClipPlayerRepository private val playerRepository: PlayerRepository,
    private val playAndAddClipsUseCase: PlayAndAddClipsUseCase,
    private val playEpisodeUseCase: PlayEpisodeUseCase,
    private val toggleLikedUseCase: ToggleLikedUseCase,
) : ViewModel() {
    private val episodes: StateFlow<List<Episode>> = getClipEpisodesUseCase(40)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    val state: StateFlow<ClipState> = combine(
        episodes,
        playerRepository.indexOfList,
        playerRepository.progress,
        playerRepository.isPlaying,
    ) { episodes, indexOfPlaying, progress, isPlaying ->
        if (episodes.isNotEmpty()) {
            ClipState.Success(episodes, indexOfPlaying, progress, isPlaying)
        } else {
            ClipState.Error("No clips available.")
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ClipState.Loading
    )

    private val _action = MutableSharedFlow<ClipAction>(extraBufferCapacity = 1)

    private val _effect = MutableSharedFlow<ClipEffect>(extraBufferCapacity = 1)
    val effect = _effect.asSharedFlow()

    init {
        playWhenReady()
        handleActions()
    }

    private fun handleActions() = viewModelScope.launch {
        _action.collectLatest { action ->
            when (action) {
                is ClipAction.PlayIndex -> playIndex(action.index)
                is ClipAction.ClickEpisode -> playEpisode(action.episode)
                is ClipAction.ToggleEpisodeLiked -> toggleEpisodeLiked(action.episode)
                is ClipAction.ClickPodcast -> clickPodcast(action.podcastId)
                is ClipAction.Resume -> resume()
                is ClipAction.Pause -> pause()
            }
        }
    }

    fun sendAction(action: ClipAction) = viewModelScope.launch {
        _action.emit(action)
    }

    private fun playWhenReady() = viewModelScope.launch {
        episodes.collectLatest { episodes ->
            playAndAddClipsUseCase(episodes)
        }
    }

    private fun playIndex(index: Int) {
        playerRepository.playIndex(index)
    }

    private fun resume() {
        playerRepository.resume()
    }

    private fun pause() {
        playerRepository.pause()
    }

    private fun playEpisode(episode: Episode) {
        playEpisodeUseCase(episode)
    }

    private fun toggleEpisodeLiked(episode: Episode) = viewModelScope.launch {
        toggleLikedUseCase(episode.id)
    }


    private fun clickPodcast(podcastId: Long) = viewModelScope.launch {
        _effect.emit(ClipEffect.NavigateToPodcast(podcastId))
    }
}

sealed interface ClipState {
    object Loading : ClipState
    data class Success(
        val episodes: List<Episode>,
        val indexOfPlaying: Int = 0,
        val progress: Progress,
        val isPlaying: Boolean,
    ) : ClipState

    data class Error(val message: String) : ClipState
}

sealed interface ClipAction {
    data class PlayIndex(val index: Int) : ClipAction
    data class ClickEpisode(val episode: Episode) : ClipAction
    data class ToggleEpisodeLiked(val episode: Episode) : ClipAction
    data class ClickPodcast(val podcastId: Long) : ClipAction
    data object Resume : ClipAction
    data object Pause : ClipAction
}

sealed interface ClipEffect {
    data class NavigateToPodcast(val podcastId: Long) : ClipEffect
}