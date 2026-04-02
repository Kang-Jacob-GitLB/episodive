package io.jacob.episodive.feature.library.navigation

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import io.jacob.episodive.feature.library.LibraryRoute
import kotlinx.serialization.Serializable

@Serializable
data object LibraryRoute : NavKey

fun EntryProviderScope<NavKey>.libraryEntries(
    onPodcastClick: (Long) -> Unit,
    onShowSnackbar: suspend (message: String, actionLabel: String?) -> Boolean,
) {
    entry<LibraryRoute> {
        LibraryRoute(
            onPodcastClick = onPodcastClick,
            onShowSnackbar = onShowSnackbar,
        )
    }
}
