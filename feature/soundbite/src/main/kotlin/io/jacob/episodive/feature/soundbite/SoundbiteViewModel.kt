package io.jacob.episodive.feature.soundbite

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.jacob.episodive.core.domain.usecase.episode.GetClipEpisodesUseCase
import io.jacob.episodive.core.model.ClipEpisode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SoundbiteViewModel @Inject constructor(
    private val getClipEpisodesUseCase: GetClipEpisodesUseCase,
) : ViewModel() {
    private val _state = MutableStateFlow<SoundbiteState>(SoundbiteState.Loading)
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            getClipEpisodesUseCase().collect { clipEpisodes ->
                if (clipEpisodes.isNotEmpty()) {
                    _state.value = SoundbiteState.Success(clipEpisodes)
                } else {
                    _state.value = SoundbiteState.Error("No soundbites available.")
                }
            }
        }
    }
}

sealed interface SoundbiteState {
    object Loading : SoundbiteState
    data class Success(val clipEpisodes: List<ClipEpisode>) : SoundbiteState
    data class Error(val message: String) : SoundbiteState
}