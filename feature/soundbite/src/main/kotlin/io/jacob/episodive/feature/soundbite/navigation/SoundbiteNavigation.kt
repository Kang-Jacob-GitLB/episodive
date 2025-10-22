package io.jacob.episodive.feature.soundbite.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavOptions
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import io.jacob.episodive.feature.soundbite.SoundbiteRoute
import kotlinx.serialization.Serializable

@Serializable
data object SoundbiteRoute

@Serializable
data object SoundbiteBaseRoute

fun NavController.navigateToSoundbite(navOptions: NavOptions) =
    navigate(route = SoundbiteBaseRoute, navOptions)

private fun NavGraphBuilder.soundbiteScreen(
    onPodcatClick: (Long) -> Unit,
    onShowSnackbar: suspend (message: String, actionLabel: String?) -> Boolean,
) {
    composable<SoundbiteRoute> {
        SoundbiteRoute(
            onPodcastClick = onPodcatClick,
            onShowSnackbar = onShowSnackbar
        )
    }
}

@Composable
fun SoundbiteNavHost(
    navController: NavHostController,
    navigateToPodcast: NavController.(Long) -> Unit,
    onShowSnackbar: suspend (message: String, actionLabel: String?) -> Boolean,
    destination: NavGraphBuilder.(NavController) -> Unit,
) {
    NavHost(
        navController = navController,
        startDestination = SoundbiteRoute
    ) {
        soundbiteScreen(
            onPodcatClick = { navController.navigateToPodcast(it) },
            onShowSnackbar = onShowSnackbar,
        )

        destination(navController)
    }
}

fun NavGraphBuilder.soundbiteSection(
    onRegisterNestedNavController: (NavHostController) -> Unit,
    navigateToPodcast: NavController.(Long) -> Unit,
    onShowSnackbar: suspend (message: String, actionLabel: String?) -> Boolean,
    destination: NavGraphBuilder.(NavController) -> Unit,
) {
    composable<SoundbiteBaseRoute> {
        val navController = rememberNavController()

        LaunchedEffect(navController) {
            onRegisterNestedNavController(navController)
        }

        SoundbiteNavHost(
            navController = navController,
            navigateToPodcast = navigateToPodcast,
            onShowSnackbar = onShowSnackbar,
            destination = destination,
        )
    }
}