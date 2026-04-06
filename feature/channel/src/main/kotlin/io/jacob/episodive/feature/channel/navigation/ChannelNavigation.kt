package io.jacob.episodive.feature.channel.navigation

import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import io.jacob.episodive.feature.channel.ChannelRoute
import io.jacob.episodive.feature.channel.ChannelViewModel
import kotlinx.serialization.Serializable

@Serializable
data class ChannelRoute(val id: Long) : NavKey

fun EntryProviderScope<NavKey>.channelEntries(
    onBackClick: () -> Unit,
    onPodcastClick: (Long) -> Unit,
    onShowSnackbar: suspend (message: String, actionLabel: String?) -> Boolean,
) {
    entry<ChannelRoute> { key ->
        ChannelRoute(
            viewModel = hiltViewModel<ChannelViewModel, ChannelViewModel.Factory>(
                creationCallback = { factory -> factory.create(key.id) }
            ),
            onBackClick = onBackClick,
            onPodcastClick = onPodcastClick,
            onShowSnackbar = onShowSnackbar,
        )
    }
}
