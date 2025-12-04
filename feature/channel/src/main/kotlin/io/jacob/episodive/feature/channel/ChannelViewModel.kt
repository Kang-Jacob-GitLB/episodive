package io.jacob.episodive.feature.channel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import io.jacob.episodive.core.domain.usecase.channel.GetChannelByIdUseCase
import io.jacob.episodive.core.domain.usecase.podcast.GetPodcastsByChannelUseCase
import io.jacob.episodive.core.model.Channel
import io.jacob.episodive.core.model.Podcast
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel(assistedFactory = ChannelViewModel.Factory::class)
class ChannelViewModel @AssistedInject constructor(
    getChannelByIdUseCase: GetChannelByIdUseCase,
    getPodcastsByChannelUseCase: GetPodcastsByChannelUseCase,
    @Assisted("id") val id: Long,
) : ViewModel() {
    @AssistedFactory
    interface Factory {
        fun create(@Assisted("id") id: Long): ChannelViewModel
    }

    private val channel = getChannelByIdUseCase(id)
    private val podcasts = channel.flatMapLatest {
        if (it == null) emptyFlow() else getPodcastsByChannelUseCase(it)
    }

    val state: StateFlow<ChannelState> = combine(
        channel,
        podcasts,
    ) { channel, podcasts ->
        if (channel == null) {
            ChannelState.Error("Channel not found")
        } else {
            ChannelState.Success(
                channel = channel,
                podcasts = podcasts,
            )
        }
    }.catch { e ->
        emit(ChannelState.Error(e.message ?: "An unknown error occurred"))
        e.printStackTrace()
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ChannelState.Loading
    )

    private val _action = MutableSharedFlow<ChannelAction>(extraBufferCapacity = 1)

    private val _effect = MutableSharedFlow<ChannelEffect>(extraBufferCapacity = 1)
    val effect = _effect.asSharedFlow()

    init {
        handleActions()
    }

    private fun handleActions() = viewModelScope.launch {
        _action.collectLatest { action ->
            when (action) {
                is ChannelAction.ClickBack -> clickBack()
                is ChannelAction.ClickPodcast -> clickPodcast(action.podcastId)
            }
        }
    }

    fun sendAction(action: ChannelAction) = viewModelScope.launch {
        _action.emit(action)
    }

    private fun clickBack() = viewModelScope.launch {
        _effect.emit(ChannelEffect.NavigateBack)
    }

    private fun clickPodcast(podcastId: Long) = viewModelScope.launch {
        _effect.emit(ChannelEffect.NavigateToPodcast(podcastId))
    }
}

sealed interface ChannelState {
    data object Loading : ChannelState
    data class Success(
        val channel: Channel,
        val podcasts: List<Podcast>,
    ) : ChannelState

    data class Error(val message: String) : ChannelState
}

sealed interface ChannelAction {
    data object ClickBack : ChannelAction
    data class ClickPodcast(val podcastId: Long) : ChannelAction
}

sealed interface ChannelEffect {
    data object NavigateBack : ChannelEffect
    data class NavigateToPodcast(val podcastId: Long) : ChannelEffect
}