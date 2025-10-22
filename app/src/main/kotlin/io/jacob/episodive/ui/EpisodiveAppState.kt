package io.jacob.episodive.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navOptions
import io.jacob.episodive.MainActivityViewModel
import io.jacob.episodive.core.data.util.NetworkMonitor
import io.jacob.episodive.feature.clip.navigation.navigateToClip
import io.jacob.episodive.feature.home.navigation.navigateToHome
import io.jacob.episodive.feature.library.navigation.navigateToLibrary
import io.jacob.episodive.feature.search.navigation.navigateToSearch
import io.jacob.episodive.navigation.BottomBarDestination
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

@Composable
fun rememberEpisodiveAppState(
    networkMonitor: NetworkMonitor,
    viewModel: MainActivityViewModel,
    navController: NavHostController = rememberNavController(),
    coroutineScope: CoroutineScope = rememberCoroutineScope(),
) = remember(
    networkMonitor,
    navController,
    coroutineScope,
) {
    EpisodiveAppState(
        networkMonitor = networkMonitor,
        viewModel = viewModel,
        navController = navController,
        coroutineScope = coroutineScope,
    )
}

class EpisodiveAppState(
    networkMonitor: NetworkMonitor,
    val viewModel: MainActivityViewModel,
    val navController: NavHostController,
    coroutineScope: CoroutineScope,
) {
    private val previousDestination = mutableStateOf<NavDestination?>(null)

    val currentDestination: StateFlow<NavDestination?> =
        navController.currentBackStackEntryFlow.map { entry ->
            entry.destination.also { destination ->
                previousDestination.value = destination
            }
        }.stateIn(
            scope = coroutineScope,
            started = SharingStarted.Eagerly,
            initialValue = null
        )

    val currentBottomBarDestination: BottomBarDestination?
        get() {
            return BottomBarDestination.entries.firstOrNull { bottomBarDestination ->
                currentDestination.value?.hasRoute(route = bottomBarDestination.baseRoute) == true
            }
        }

    val bottomBarDestinations: List<BottomBarDestination> = BottomBarDestination.entries
    val startDestination = bottomBarDestinations.first()

    private val nestedNavControllers = mutableMapOf<BottomBarDestination, NavHostController>()
    val currentNestedNavController: NavHostController?
        get() = currentBottomBarDestination?.let { nestedNavControllers[it] }

    fun registerNestedNavController(
        destination: BottomBarDestination,
        navController: NavHostController
    ) {
        nestedNavControllers[destination] = navController
    }

    private fun navigateToNestedNavRoot(destination: BottomBarDestination) {
        nestedNavControllers[destination]?.popBackStack(
            route = destination.route,
            inclusive = false
        )
    }

    fun navigateToBottomBarDestination(destination: BottomBarDestination) {
        // 같은 탭을 다시 클릭한 경우 해당 탭의 root로 이동
        if (currentDestination.value?.hasRoute(destination.baseRoute) == true) {
            navigateToNestedNavRoot(destination)
            return
        }

        val bottomBarNavOptions = navOptions {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }

        when (destination) {
            BottomBarDestination.HOME -> navController.navigateToHome(bottomBarNavOptions)
            BottomBarDestination.SEARCH -> navController.navigateToSearch(bottomBarNavOptions)
            BottomBarDestination.LIBRARY -> navController.navigateToLibrary(bottomBarNavOptions)
            BottomBarDestination.CLIP -> navController.navigateToClip(bottomBarNavOptions)
        }
    }

    fun navigateToBottomBarStartDestination() {
        val bottomBarNavOptions = navOptions {
            popUpTo(navController.graph.id) {
                inclusive = true
            }
            launchSingleTop = true
        }

        when (startDestination) {
            BottomBarDestination.HOME -> navController.navigateToHome(bottomBarNavOptions)
            BottomBarDestination.SEARCH -> navController.navigateToSearch(bottomBarNavOptions)
            BottomBarDestination.LIBRARY -> navController.navigateToLibrary(bottomBarNavOptions)
            BottomBarDestination.CLIP -> navController.navigateToClip(bottomBarNavOptions)
        }
    }

    val isOffline = networkMonitor.isOnline
        .map(Boolean::not)
        .stateIn(
            scope = coroutineScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = false,
        )
}