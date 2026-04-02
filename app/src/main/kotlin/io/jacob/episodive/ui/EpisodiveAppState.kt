package io.jacob.episodive.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import io.jacob.episodive.MainActivityViewModel
import io.jacob.episodive.core.data.util.NetworkMonitor
import io.jacob.episodive.feature.home.navigation.HomeRoute
import io.jacob.episodive.feature.podcast.navigation.PodcastRoute
import io.jacob.episodive.navigation.BottomBarDestination
import io.jacob.episodive.navigation.EpisodiveNavigationState
import io.jacob.episodive.navigation.EpisodiveNavigator
import io.jacob.episodive.navigation.rememberEpisodiveNavigationState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

@Composable
fun rememberEpisodiveAppState(
    networkMonitor: NetworkMonitor,
    viewModel: MainActivityViewModel,
    coroutineScope: CoroutineScope = rememberCoroutineScope(),
): EpisodiveAppState {
    val navigationState = rememberEpisodiveNavigationState(
        startRoute = HomeRoute,
        topLevelRoutes = BottomBarDestination.entries.map { it.navKey }.toSet(),
    )
    val navigator = remember(navigationState) { EpisodiveNavigator(navigationState) }

    return remember(networkMonitor, navigationState, navigator, coroutineScope) {
        EpisodiveAppState(
            networkMonitor = networkMonitor,
            viewModel = viewModel,
            navigationState = navigationState,
            navigator = navigator,
            coroutineScope = coroutineScope,
        )
    }
}

class EpisodiveAppState(
    networkMonitor: NetworkMonitor,
    val viewModel: MainActivityViewModel,
    val navigationState: EpisodiveNavigationState,
    val navigator: EpisodiveNavigator,
    coroutineScope: CoroutineScope,
) {
    val bottomBarDestinations: List<BottomBarDestination> = BottomBarDestination.entries

    fun navigateToBottomBarDestination(destination: BottomBarDestination) {
        val route = destination.navKey
        if (route == navigationState.topLevelRoute) {
            navigator.navigateToTabRoot()
        } else {
            navigator.navigate(route)
        }
    }

    fun navigateToPodcast(podcastId: Long) {
        navigator.navigate(PodcastRoute(podcastId))
    }

    val isOffline = networkMonitor.isOnline
        .map(Boolean::not)
        .stateIn(
            scope = coroutineScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = false,
        )
}
