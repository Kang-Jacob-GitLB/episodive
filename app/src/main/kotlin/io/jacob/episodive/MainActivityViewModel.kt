package io.jacob.episodive

import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.jacob.episodive.core.domain.usecase.user.GetUserDataUseCase
import io.jacob.episodive.sync.EpisodeSyncNotificationHelper
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainActivityViewModel @Inject constructor(
    getUserDataUseCase: GetUserDataUseCase,
) : ViewModel() {
    val state: StateFlow<MainActivityState> = getUserDataUseCase().map { userData ->
        if (userData.isFirstLaunch) {
            MainActivityState.FirstLaunch
        } else {
            MainActivityState.NotFirstLaunch
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = MainActivityState.Loading,
    )

    private val _deepLinkEvent = MutableSharedFlow<DeepLinkEvent>(replay = 1)
    val deepLinkEvent: SharedFlow<DeepLinkEvent> = _deepLinkEvent.asSharedFlow()

    fun handleDeepLink(intent: Intent?) {
        val podcastId = intent?.getLongExtra(EpisodeSyncNotificationHelper.EXTRA_PODCAST_ID, -1L) ?: -1L
        if (podcastId > 0) {
            viewModelScope.launch { _deepLinkEvent.emit(DeepLinkEvent.Podcast(podcastId)) }
        }
    }

    fun consumeDeepLink() {
        _deepLinkEvent.resetReplayCache()
    }
}

sealed interface DeepLinkEvent {
    data class Podcast(val id: Long) : DeepLinkEvent
}

sealed interface MainActivityState {
    data object Loading : MainActivityState
    data object FirstLaunch : MainActivityState
    data object NotFirstLaunch : MainActivityState

    fun shouldKeepSplashScreen() = this is Loading
    fun isFirstLaunch() = this is FirstLaunch
}