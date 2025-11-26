package io.jacob.episodive.core.designsystem.component

import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.jacob.episodive.core.designsystem.icon.EpisodiveIcons
import io.jacob.episodive.core.designsystem.theme.EpisodiveTheme
import io.jacob.episodive.core.designsystem.tooling.DevicePreviews

@Composable
fun RowScope.EpisodiveNavigationBarItem(
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    alwaysShowLabel: Boolean = true,
    icon: @Composable () -> Unit,
    selectedIcon: @Composable () -> Unit = icon,
    label: @Composable (() -> Unit)? = null,
) {
    NavigationBarItem(
        selected = selected,
        onClick = onClick,
        icon = if (selected) selectedIcon else icon,
        modifier = modifier,
        enabled = enabled,
        label = label,
        alwaysShowLabel = alwaysShowLabel,
        colors = NavigationBarItemDefaults.colors(
            selectedIconColor = EpisodiveNavigationDefaults.navigationSelectedItemColor(),
            unselectedIconColor = EpisodiveNavigationDefaults.navigationContentColor(),
            selectedTextColor = EpisodiveNavigationDefaults.navigationSelectedItemColor(),
            unselectedTextColor = EpisodiveNavigationDefaults.navigationContentColor(),
            indicatorColor = EpisodiveNavigationDefaults.navigationIndicatorColor(),
        ),
    )
}

@Composable
fun EpisodiveNavigationBar(
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit,
) {
    NavigationBar(
        modifier = modifier,
        contentColor = EpisodiveNavigationDefaults.navigationContentColor(),
        tonalElevation = 0.dp,
        content = content
    )
}

@DevicePreviews
@Composable
private fun EpisodiveNavigationBarPreview() {
    val items = listOf("Home", "Search", "Library", "Clip")
    val selectedIcons = listOf(
        EpisodiveIcons.HomeFilled,
        EpisodiveIcons.SearchFilled,
        EpisodiveIcons.LibraryFilled,
        EpisodiveIcons.ClipFilled,
    )
    val unselectedIcon = listOf(
        EpisodiveIcons.Home,
        EpisodiveIcons.Search,
        EpisodiveIcons.Library,
        EpisodiveIcons.Clip,
    )

    EpisodiveTheme {
        EpisodiveNavigationBar {
            items.forEachIndexed { index, item ->
                EpisodiveNavigationBarItem(
                    icon = {
                        Icon(
                            imageVector = unselectedIcon[index],
                            contentDescription = null
                        )
                    },
                    selectedIcon = {
                        Icon(
                            imageVector = selectedIcons[index],
                            contentDescription = null
                        )
                    },
                    label = {
                        Text(
                            text = item,
                            style = MaterialTheme.typography.labelLarge,
                            maxLines = 1
                        )
                    },
                    selected = index == 0,
                    onClick = { },
                )
            }
        }
    }
}

object EpisodiveNavigationDefaults {
    @Composable
    fun navigationContentColor() = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)

    @Composable
    fun navigationSelectedItemColor() = MaterialTheme.colorScheme.onPrimaryContainer

    @Composable
    fun navigationIndicatorColor() = MaterialTheme.colorScheme.primaryContainer
}