package io.jacob.episodive.feature.library.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavOptions
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import io.jacob.episodive.feature.library.LibraryRoute
import kotlinx.serialization.Serializable

@Serializable
data object LibraryRoute

@Serializable
data object LibraryBaseRoute

fun NavController.navigateToLibrary(navOptions: NavOptions) =
    navigate(route = LibraryBaseRoute, navOptions)

private fun NavGraphBuilder.libraryScreen(
    onPodcatClick: (Long) -> Unit,
//    onStoryClick: (Story) -> Unit,
    onShowSnackbar: suspend (message: String, actionLabel: String?) -> Boolean,
) {
    composable<LibraryRoute> {
        LibraryRoute(
            onPodcastClick = onPodcatClick,
//            onStoryClick = onStoryClick,
            onShowSnackbar = onShowSnackbar
        )
    }
}

@Composable
private fun LibraryNavHost(
    navController: NavHostController,
    navigateToPodcast: NavController.(Long) -> Unit,
//    navigateToStoryDetail: NavController.(Story) -> Unit,
    onShowSnackbar: suspend (message: String, actionLabel: String?) -> Boolean,
    destination: NavGraphBuilder.(NavController) -> Unit,
) {
    NavHost(
        navController = navController,
        startDestination = LibraryRoute
    ) {
        libraryScreen(
            onPodcatClick = { navController.navigateToPodcast(it) },
//            onStoryClick = { navController.navigateToStoryDetail(it) },
            onShowSnackbar = onShowSnackbar,
        )

        destination(navController)
    }
}

fun NavGraphBuilder.librarySection(
    onRegisterNestedNavController: (NavHostController) -> Unit,
    navigateToPodcast: NavController.(Long) -> Unit,
//    navigateToStoryDetail: NavController.(Story) -> Unit,
    onShowSnackbar: suspend (message: String, actionLabel: String?) -> Boolean,
    destination: NavGraphBuilder.(NavController) -> Unit,
) {
    composable<LibraryBaseRoute> {
        val navController = rememberNavController()

        LaunchedEffect(navController) {
            onRegisterNestedNavController(navController)
        }

        LibraryNavHost(
            navController = navController,
            navigateToPodcast = navigateToPodcast,
//            navigateToStoryDetail = navigateToStoryDetail,
            onShowSnackbar = onShowSnackbar,
            destination = destination
        )
    }
}