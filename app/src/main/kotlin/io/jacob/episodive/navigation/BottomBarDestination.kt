package io.jacob.episodive.navigation

import androidx.annotation.StringRes
import androidx.compose.ui.graphics.vector.ImageVector
import io.jacob.episodive.core.designsystem.icon.EpisodiveIcons
import io.jacob.episodive.feature.clip.navigation.ClipBaseRoute
import io.jacob.episodive.feature.clip.navigation.ClipRoute
import io.jacob.episodive.feature.home.navigation.HomeBaseRoute
import io.jacob.episodive.feature.home.navigation.HomeRoute
import io.jacob.episodive.feature.library.navigation.LibraryBaseRoute
import io.jacob.episodive.feature.library.navigation.LibraryRoute
import io.jacob.episodive.feature.search.navigation.SearchBaseRoute
import io.jacob.episodive.feature.search.navigation.SearchRoute
import kotlin.reflect.KClass
import io.jacob.episodive.feature.clip.R as clipR
import io.jacob.episodive.feature.home.R as homeR
import io.jacob.episodive.feature.library.R as libraryR
import io.jacob.episodive.feature.search.R as searchR

enum class BottomBarDestination(
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    @StringRes val iconTextId: Int,
    @StringRes val titleTextId: Int = iconTextId,
    val route: KClass<*>,
    val baseRoute: KClass<*> = route,
) {
    HOME(
        selectedIcon = EpisodiveIcons.Home,
        unselectedIcon = EpisodiveIcons.HomeBorder,
        iconTextId = homeR.string.feature_home_title,
        route = HomeRoute::class,
        baseRoute = HomeBaseRoute::class,
    ),
    SEARCH(
        selectedIcon = EpisodiveIcons.Search,
        unselectedIcon = EpisodiveIcons.SearchBorder,
        iconTextId = searchR.string.feature_search_title,
        route = SearchRoute::class,
        baseRoute = SearchBaseRoute::class,
    ),
    LIBRARY(
        selectedIcon = EpisodiveIcons.LibraryBooks,
        unselectedIcon = EpisodiveIcons.LibraryBooksBorder,
        iconTextId = libraryR.string.feature_library_title,
        route = LibraryRoute::class,
        baseRoute = LibraryBaseRoute::class,
    ),
    CLIP(
        selectedIcon = EpisodiveIcons.AutoAwesome,
        unselectedIcon = EpisodiveIcons.AutoAwesomeBorder,
        iconTextId = clipR.string.feature_clip_title,
        route = ClipRoute::class,
        baseRoute = ClipBaseRoute::class,
    ),
}