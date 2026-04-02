package io.jacob.episodive.feature.search.navigation

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import io.jacob.episodive.feature.search.SearchRoute
import kotlinx.serialization.Serializable

@Serializable
data object SearchRoute : NavKey

fun EntryProviderScope<NavKey>.searchEntries(
    onPodcastClick: (Long) -> Unit,
    onShowSnackbar: suspend (message: String, actionLabel: String?) -> Boolean,
) {
    entry<SearchRoute> {
        SearchRoute(
            onPodcastClick = onPodcastClick,
            onShowSnackbar = onShowSnackbar,
        )
    }
}
