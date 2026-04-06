package io.jacob.episodive.feature.clip.navigation

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import io.jacob.episodive.feature.clip.ClipRoute
import kotlinx.serialization.Serializable

@Serializable
data object ClipRoute : NavKey

fun EntryProviderScope<NavKey>.clipEntries(
    onPodcastClick: (Long) -> Unit,
    onShowSnackbar: suspend (message: String, actionLabel: String?) -> Boolean,
) {
    entry<ClipRoute> {
        ClipRoute(
            onPodcastClick = onPodcastClick,
            onShowSnackbar = onShowSnackbar,
        )
    }
}
