package io.jacob.episodive.feature.radio.navigation

import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import io.jacob.episodive.feature.radio.RadioRoute
import kotlinx.serialization.Serializable

@Serializable
data object RadioRoute : NavKey

fun EntryProviderScope<NavKey>.radioEntries(
    onBackClick: () -> Unit,
    onShowSnackbar: suspend (message: String, actionLabel: String?) -> Boolean,
) {
    entry<RadioRoute> {
        RadioRoute(
            viewModel = hiltViewModel(),
            onBackClick = onBackClick,
            onShowSnackbar = onShowSnackbar,
        )
    }
}
