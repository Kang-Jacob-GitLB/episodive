package io.jacob.episodive.feature.soundbite

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.jacob.episodive.core.domain.di.ClipPlayerRepository
import io.jacob.episodive.core.domain.repository.PlayerRepository
import io.jacob.episodive.core.domain.usecase.episode.GetClipEpisodesUseCase
import io.jacob.episodive.core.domain.usecase.player.PlayEpisodeUseCase
import io.jacob.episodive.core.model.ClipEpisode
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
class SoundbiteViewModel @Inject constructor(
    private val getClipEpisodesUseCase: GetClipEpisodesUseCase,
    @ClipPlayerRepository private val playerRepository: PlayerRepository,
    private val playEpisodeUseCase: PlayEpisodeUseCase,
) : ViewModel() {
    private val clipEpisodes: StateFlow<List<ClipEpisode>> = getClipEpisodesUseCase()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    val state: StateFlow<SoundbiteState> = combine(
        clipEpisodes,
        playerRepository.indexOfList,
        playerRepository.progress,
        playerRepository.isPlaying,
    ) { episodes, indexOfPlaying, progress, isPlaying ->
        if (episodes.isNotEmpty()) {
            SoundbiteState.Success(episodes, indexOfPlaying, progress, isPlaying)
        } else {
            SoundbiteState.Error("No soundbites available.")
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SoundbiteState.Loading
    )

    private val _action = MutableSharedFlow<SoundbiteAction>(extraBufferCapacity = 1)

    private val _effect = MutableSharedFlow<SoundbiteEffect>(extraBufferCapacity = 1)
    val effect = _effect.asSharedFlow()

    init {
        playWhenReady()
        handleActions()
    }

    private fun handleActions() = viewModelScope.launch {
        _action.collectLatest { action ->
            when (action) {
                is SoundbiteAction.PlayIndex -> playIndex(action.index)
                is SoundbiteAction.ClickEpisode -> playEpisode(action.episode)
                is SoundbiteAction.ClickPodcast -> clickPodcast(action.podcastId)
                is SoundbiteAction.Resume -> resume()
                is SoundbiteAction.Pause -> pause()
            }
        }
    }

    fun sendAction(action: SoundbiteAction) = viewModelScope.launch {
        _action.emit(action)
    }

    private fun playWhenReady() = viewModelScope.launch {
        clipEpisodes.collectLatest {
            if (it.isNotEmpty()) {
                playerRepository.playClips(
                    clipEpisodes = it,
                    indexToPlay = 0,
                )
            }
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

    private fun clickPodcast(podcastId: Long) = viewModelScope.launch {
        _effect.emit(SoundbiteEffect.NavigateToPodcast(podcastId))
    }
}

sealed interface SoundbiteState {
    object Loading : SoundbiteState
    data class Success(
        val clipEpisodes: List<ClipEpisode>,
        val indexOfPlaying: Int = 0,
        val progress: Progress,
        val isPlaying: Boolean,
    ) : SoundbiteState

    data class Error(val message: String) : SoundbiteState
}

sealed interface SoundbiteAction {
    data class PlayIndex(val index: Int) : SoundbiteAction
    data class ClickEpisode(val episode: Episode) : SoundbiteAction
    data class ClickPodcast(val podcastId: Long) : SoundbiteAction
    data object Resume : SoundbiteAction
    data object Pause : SoundbiteAction
}

sealed interface SoundbiteEffect {
    data class NavigateToPodcast(val podcastId: Long) : SoundbiteEffect
}