package io.jacob.episodive.feature.home.navigation

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import io.jacob.episodive.feature.home.HomeRoute
import kotlinx.serialization.Serializable

@Serializable
data object HomeRoute : NavKey

fun EntryProviderScope<NavKey>.homeEntries(
    onPodcastClick: (Long) -> Unit,
    onChannelClick: (Long) -> Unit,
    onShowSnackbar: suspend (message: String, actionLabel: String?) -> Boolean,
) {
    entry<HomeRoute> {
        HomeRoute(
            onPodcastClick = onPodcastClick,
            onChannelClick = onChannelClick,
            onShowSnackbar = onShowSnackbar,
        )
    }
}
