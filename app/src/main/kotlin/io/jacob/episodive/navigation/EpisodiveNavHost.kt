package io.jacob.episodive.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import io.jacob.episodive.feature.channel.navigation.ChannelRoute
import io.jacob.episodive.feature.channel.navigation.channelEntries
import io.jacob.episodive.feature.clip.navigation.clipEntries
import io.jacob.episodive.feature.home.navigation.homeEntries
import io.jacob.episodive.feature.library.navigation.libraryEntries
import io.jacob.episodive.feature.podcast.navigation.PodcastRoute
import io.jacob.episodive.feature.podcast.navigation.podcastEntries
import io.jacob.episodive.feature.search.navigation.searchEntries

@Composable
fun EpisodiveNavHost(
    navigationState: EpisodiveNavigationState,
    navigator: EpisodiveNavigator,
    onShowSnackbar: suspend (message: String, actionLabel: String?) -> Boolean,
    modifier: Modifier = Modifier,
) {
    val entryProvider = entryProvider<NavKey> {
        homeEntries(
            onPodcastClick = { navigator.navigate(PodcastRoute(it)) },
            onChannelClick = { navigator.navigate(ChannelRoute(it)) },
            onShowSnackbar = onShowSnackbar,
        )
        searchEntries(
            onPodcastClick = { navigator.navigate(PodcastRoute(it)) },
            onShowSnackbar = onShowSnackbar,
        )
        libraryEntries(
            onPodcastClick = { navigator.navigate(PodcastRoute(it)) },
            onShowSnackbar = onShowSnackbar,
        )
        clipEntries(
            onPodcastClick = { navigator.navigate(PodcastRoute(it)) },
            onShowSnackbar = onShowSnackbar,
        )
        podcastEntries(
            onBackClick = { navigator.goBack() },
            onShowSnackbar = onShowSnackbar,
        )
        channelEntries(
            onBackClick = { navigator.goBack() },
            onPodcastClick = { navigator.navigate(PodcastRoute(it)) },
            onShowSnackbar = onShowSnackbar,
        )
    }

    NavDisplay(
        entries = navigationState.toDecoratedEntries(entryProvider),
        onBack = { navigator.goBack() },
        modifier = modifier,
    )
}
