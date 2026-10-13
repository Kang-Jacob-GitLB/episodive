package io.jacob.episodive.feature.home.navigation

import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import io.jacob.episodive.feature.home.HomeMoreRoute
import io.jacob.episodive.feature.home.HomeMoreViewModel
import io.jacob.episodive.feature.home.HomeRoute
import kotlinx.serialization.Serializable

@Serializable
data object HomeRoute : NavKey

/** 홈 섹션의 '더 보기' 목적지. 어느 섹션인지는 [section] 이 전부 결정한다. */
@Serializable
data class HomeMoreRoute(val section: HomeSection) : NavKey

fun EntryProviderScope<NavKey>.homeEntries(
    onPodcastClick: (Long) -> Unit,
    onChannelClick: (Long) -> Unit,
    onMoreClick: (HomeSection) -> Unit,
    onRadioClick: () -> Unit,
    onBackClick: () -> Unit,
    onSearchClick: () -> Unit,
    onShowSnackbar: suspend (message: String, actionLabel: String?) -> Boolean,
) {
    entry<HomeRoute> {
        HomeRoute(
            onPodcastClick = onPodcastClick,
            onChannelClick = onChannelClick,
            onMoreClick = onMoreClick,
            onRadioClick = onRadioClick,
            onSearchClick = onSearchClick,
            onShowSnackbar = onShowSnackbar,
        )
    }

    entry<HomeMoreRoute> { key ->
        HomeMoreRoute(
            viewModel = hiltViewModel<HomeMoreViewModel, HomeMoreViewModel.Factory>(
                creationCallback = { factory -> factory.create(key.section) }
            ),
            onBackClick = onBackClick,
            onPodcastClick = onPodcastClick,
            onChannelClick = onChannelClick,
            onShowSnackbar = onShowSnackbar,
        )
    }
}
