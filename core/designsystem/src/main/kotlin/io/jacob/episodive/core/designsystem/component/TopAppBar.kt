package io.jacob.episodive.core.designsystem.component

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarColors
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import io.jacob.episodive.core.designsystem.icon.EpisodiveIcons
import io.jacob.episodive.core.designsystem.theme.EpisodiveTheme
import io.jacob.episodive.core.designsystem.tooling.ThemePreviews

@Composable
fun EpisodiveTopAppBar(
    modifier: Modifier = Modifier,
    title: @Composable () -> Unit,
    navigationIcon: ImageVector? = null,
    navigationIconContentDescription: String? = null,
    actionIcon: ImageVector? = null,
    actionIconContentDescription: String? = null,
    colors: TopAppBarColors = TopAppBarDefaults.topAppBarColors(),
    scrollBehavior: TopAppBarScrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(),
    onNavigationClick: () -> Unit = {},
    onActionClick: () -> Unit = {},
) {
    TopAppBar(
        title = title,
        navigationIcon = {
            if (navigationIcon == null) return@TopAppBar
            if (navigationIconContentDescription == null) return@TopAppBar

            IconButton(onClick = onNavigationClick) {
                Icon(
                    imageVector = navigationIcon,
                    contentDescription = navigationIconContentDescription,
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }
        },
        actions = {
            if (actionIcon == null) return@TopAppBar
            if (actionIconContentDescription == null) return@TopAppBar

            IconButton(onClick = onActionClick) {
                Icon(
                    imageVector = actionIcon,
                    contentDescription = actionIconContentDescription,
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }
        },
        colors = colors,
        scrollBehavior = scrollBehavior,
        modifier = modifier.testTag("episodiveTopAppBar"),
    )
}

@Composable
fun EpisodiveCenterTopAppBar(
    modifier: Modifier = Modifier,
    title: @Composable () -> Unit,
    navigationIcon: ImageVector? = null,
    navigationIconContentDescription: String? = null,
    actionIcon: ImageVector? = null,
    actionIconContentDescription: String? = null,
    colors: TopAppBarColors = TopAppBarDefaults.topAppBarColors(),
    windowInsets: WindowInsets = TopAppBarDefaults.windowInsets,
    scrollBehavior: TopAppBarScrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(),
    onNavigationClick: () -> Unit = {},
    onActionClick: () -> Unit = {},
) {
    CenterAlignedTopAppBar(
        title = title,
        navigationIcon = {
            if (navigationIcon == null) return@CenterAlignedTopAppBar
            if (navigationIconContentDescription == null) return@CenterAlignedTopAppBar

            IconButton(onClick = onNavigationClick) {
                Icon(
                    imageVector = navigationIcon,
                    contentDescription = navigationIconContentDescription,
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }
        },
        actions = {
            if (actionIcon == null) return@CenterAlignedTopAppBar
            if (actionIconContentDescription == null) return@CenterAlignedTopAppBar

            IconButton(onClick = onActionClick) {
                Icon(
                    imageVector = actionIcon,
                    contentDescription = actionIconContentDescription,
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }
        },
        colors = colors,
        scrollBehavior = scrollBehavior,
        modifier = modifier.testTag("episodiveCenterTopAppBar"),
        windowInsets = windowInsets,
    )
}

@ThemePreviews
@Composable
private fun EpisodiveTopAppBarPreview() {
    EpisodiveTheme {
        EpisodiveTopAppBar(
            title = { Text(text = "Title") },
            navigationIcon = EpisodiveIcons.Search,
            navigationIconContentDescription = "Navigation icon",
            actionIcon = EpisodiveIcons.MoreVert,
            actionIconContentDescription = "Action icon",
        )
    }
}

@ThemePreviews
@Composable
private fun EpisodiveCenterTopAppBarPreview() {
    EpisodiveTheme {
        EpisodiveCenterTopAppBar(
            title = { Text(text = "Title") },
            navigationIcon = EpisodiveIcons.Search,
            navigationIconContentDescription = "Navigation icon",
            actionIcon = EpisodiveIcons.MoreVert,
            actionIconContentDescription = "Action icon",
        )
    }
}