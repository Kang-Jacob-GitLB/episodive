package io.jacob.episodive.navigation

import androidx.annotation.StringRes
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation3.runtime.NavKey
import io.jacob.episodive.core.designsystem.icon.EpisodiveIcons
import io.jacob.episodive.feature.clip.navigation.ClipRoute
import io.jacob.episodive.feature.home.navigation.HomeRoute
import io.jacob.episodive.feature.library.navigation.LibraryRoute
import io.jacob.episodive.feature.search.navigation.SearchRoute
import io.jacob.episodive.feature.clip.R as clipR
import io.jacob.episodive.feature.home.R as homeR
import io.jacob.episodive.feature.library.R as libraryR
import io.jacob.episodive.feature.search.R as searchR

enum class BottomBarDestination(
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    @StringRes val iconTextId: Int,
    @StringRes val titleTextId: Int = iconTextId,
    val navKey: NavKey,
) {
    HOME(
        selectedIcon = EpisodiveIcons.HomeFilled,
        unselectedIcon = EpisodiveIcons.Home,
        iconTextId = homeR.string.feature_home_title,
        navKey = HomeRoute,
    ),
    SEARCH(
        selectedIcon = EpisodiveIcons.SearchFilled,
        unselectedIcon = EpisodiveIcons.Search,
        iconTextId = searchR.string.feature_search_title,
        navKey = SearchRoute,
    ),
    LIBRARY(
        selectedIcon = EpisodiveIcons.LibraryFilled,
        unselectedIcon = EpisodiveIcons.Library,
        iconTextId = libraryR.string.feature_library_title,
        navKey = LibraryRoute,
    ),
    CLIP(
        selectedIcon = EpisodiveIcons.ClipFilled,
        unselectedIcon = EpisodiveIcons.Clip,
        iconTextId = clipR.string.feature_clip_title,
        navKey = ClipRoute,
    ),
}
