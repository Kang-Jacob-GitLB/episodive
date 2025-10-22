package io.jacob.episodive.feature.clip.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavOptions
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import io.jacob.episodive.feature.clip.ClipRoute
import kotlinx.serialization.Serializable

@Serializable
data object ClipRoute

@Serializable
data object ClipBaseRoute

fun NavController.navigateToClip(navOptions: NavOptions) =
    navigate(route = ClipBaseRoute, navOptions)

private fun NavGraphBuilder.clipScreen(
    onPodcatClick: (Long) -> Unit,
    onShowSnackbar: suspend (message: String, actionLabel: String?) -> Boolean,
) {
    composable<ClipRoute> {
        ClipRoute(
            onPodcastClick = onPodcatClick,
            onShowSnackbar = onShowSnackbar
        )
    }
}

@Composable
private fun ClipNavHost(
    navController: NavHostController,
    navigateToPodcast: NavController.(Long) -> Unit,
    onShowSnackbar: suspend (message: String, actionLabel: String?) -> Boolean,
    destination: NavGraphBuilder.(NavController) -> Unit,
) {
    NavHost(
        navController = navController,
        startDestination = ClipRoute
    ) {
        clipScreen(
            onPodcatClick = { navController.navigateToPodcast(it) },
            onShowSnackbar = onShowSnackbar,
        )

        destination(navController)
    }
}

fun NavGraphBuilder.clipSection(
    onRegisterNestedNavController: (NavHostController) -> Unit,
    navigateToPodcast: NavController.(Long) -> Unit,
    onShowSnackbar: suspend (message: String, actionLabel: String?) -> Boolean,
    destination: NavGraphBuilder.(NavController) -> Unit,
) {
    composable<ClipBaseRoute> {
        val nestedNavController = rememberNavController()

        LaunchedEffect(nestedNavController) {
            onRegisterNestedNavController(nestedNavController)
        }

        ClipNavHost(
            navController = nestedNavController,
            navigateToPodcast = navigateToPodcast,
            onShowSnackbar = onShowSnackbar,
            destination = destination
        )
    }
}