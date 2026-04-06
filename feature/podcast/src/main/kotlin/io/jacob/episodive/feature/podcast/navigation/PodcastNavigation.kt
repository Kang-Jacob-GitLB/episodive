package io.jacob.episodive.feature.podcast.navigation

import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import io.jacob.episodive.feature.podcast.PodcastRoute
import io.jacob.episodive.feature.podcast.PodcastViewModel
import kotlinx.serialization.Serializable

@Serializable
data class PodcastRoute(val id: Long) : NavKey

fun EntryProviderScope<NavKey>.podcastEntries(
    onBackClick: () -> Unit,
    onShowSnackbar: suspend (message: String, actionLabel: String?) -> Boolean,
) {
    entry<PodcastRoute> { key ->
        PodcastRoute(
            viewModel = hiltViewModel<PodcastViewModel, PodcastViewModel.Factory>(
                creationCallback = { factory -> factory.create(key.id) }
            ),
            onBackClick = onBackClick,
            onShowSnackbar = onShowSnackbar,
        )
    }
}
