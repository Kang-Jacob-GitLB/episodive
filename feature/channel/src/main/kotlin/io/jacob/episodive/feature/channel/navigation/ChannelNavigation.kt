package io.jacob.episodive.feature.channel.navigation

import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptionsBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import io.jacob.episodive.feature.channel.ChannelRoute
import io.jacob.episodive.feature.channel.ChannelViewModel
import kotlinx.serialization.Serializable

@Serializable
data class ChannelRoute(val id: Long)

fun NavController.navigateToChannel(
    channelId: Long, navOptions: NavOptionsBuilder.() -> Unit = {},
) = navigate(route = ChannelRoute(channelId), navOptions)

fun NavGraphBuilder.channelScreen(
    onBackClick: () -> Unit,
    onPodcastClick: (Long) -> Unit,
    onShowSnackbar: suspend (message: String, actionLabel: String?) -> Boolean,
) {
    composable<ChannelRoute> { entry ->
        val id = entry.toRoute<ChannelRoute>().id
        ChannelRoute(
            viewModel = hiltViewModel<ChannelViewModel, ChannelViewModel.Factory>(
                key = "channel:$id"
            ) { factory ->
                factory.create(id)
            },
            onBackClick = onBackClick,
            onPodcastClick = onPodcastClick,
            onShowSnackbar = onShowSnackbar
        )
    }
}