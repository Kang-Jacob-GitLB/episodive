package io.jacob.episodive.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconToggleButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonColors
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.IconToggleButtonColors
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import io.jacob.episodive.core.designsystem.icon.EpisodiveIcons
import io.jacob.episodive.core.designsystem.theme.EpisodiveTheme
import io.jacob.episodive.core.designsystem.tooling.ThemePreviews

@Composable
fun EpisodiveIconToggleButton(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = CircleShape,
    colors: IconToggleButtonColors = IconButtonDefaults.iconToggleButtonColors(
        checkedContainerColor = MaterialTheme.colorScheme.primary,
        checkedContentColor = MaterialTheme.colorScheme.onPrimary,
        containerColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.1f),
        contentColor = MaterialTheme.colorScheme.onBackground,
        disabledContainerColor = if (checked) {
            MaterialTheme.colorScheme.onBackground.copy(
                alpha = EpisodiveIconButtonDefaults.DISABLED_ICON_BUTTON_CONTAINER_ALPHA,
            )
        } else {
            Color.Transparent
        },
    ),
    icon: @Composable () -> Unit,
    checkedIcon: @Composable () -> Unit = icon,
) {
    FilledIconToggleButton(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = modifier,
        enabled = enabled,
        shape = shape,
        colors = colors,
    ) {
        if (checked) checkedIcon() else icon()
    }
}

@Composable
fun EpisodiveIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = CircleShape,
    colors: IconButtonColors = IconButtonDefaults.iconButtonColors(
        containerColor = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.onBackground,
        disabledContainerColor = MaterialTheme.colorScheme.onBackground.copy(
            alpha = EpisodiveIconButtonDefaults.DISABLED_ICON_BUTTON_CONTAINER_ALPHA,
        ),
    ),
    icon: @Composable () -> Unit,
) {
    IconButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        shape = shape,
        colors = colors,
    ) {
        icon()
    }
}

@Composable
fun EpisodiveIconProgressButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    progress: Float = 0f,
    colors: IconButtonColors = IconButtonDefaults.iconButtonColors(
        containerColor = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.primary,
        disabledContainerColor = MaterialTheme.colorScheme.onBackground.copy(
            alpha = EpisodiveIconButtonDefaults.DISABLED_ICON_BUTTON_CONTAINER_ALPHA,
        ),
        disabledContentColor = MaterialTheme.colorScheme.onBackground.copy(
            alpha = EpisodiveIconButtonDefaults.DISABLED_ICON_BUTTON_CONTAINER_ALPHA,
        ),
    ),
    icon: @Composable () -> Unit,
) {
    IconButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        shape = CircleShape,
        colors = IconButtonDefaults.iconButtonColors(
            containerColor = Color.Transparent,
            contentColor = colors.contentColor,
            disabledContainerColor = Color.Transparent,
            disabledContentColor = colors.disabledContentColor,
        )
    ) {
        Box(
            modifier = Modifier,
            contentAlignment = Alignment.Center
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.fillMaxSize(),
                    color = if (enabled) colors.containerColor else colors.disabledContentColor,
                    trackColor = Color.Transparent,
                    strokeWidth = 2.dp
                )
            } else {
                CircularProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxSize(),
                    color = if (enabled) colors.containerColor else colors.disabledContentColor,
                    trackColor = Color.Transparent,
                    strokeWidth = 2.dp
                )
            }

            icon()
        }
    }
}

@ThemePreviews
@Composable
private fun EpisodiveIconToggleButtonPreview() {
    EpisodiveTheme {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            EpisodiveIconToggleButton(
                checked = true,
                onCheckedChange = { },
                icon = {
                    Icon(
                        imageVector = EpisodiveIcons.Add,
                        contentDescription = null,
                    )
                },
                checkedIcon = {
                    Icon(
                        imageVector = EpisodiveIcons.Check,
                        contentDescription = null,
                    )
                },
            )
            EpisodiveIconToggleButton(
                checked = false,
                onCheckedChange = { },
                icon = {
                    Icon(
                        imageVector = EpisodiveIcons.Add,
                        contentDescription = null,
                    )
                },
                checkedIcon = {
                    Icon(
                        imageVector = EpisodiveIcons.Check,
                        contentDescription = null,
                    )
                },
            )
        }
    }
}

@ThemePreviews
@Composable
private fun EpisodiveIconButtonPreview() {
    EpisodiveTheme {
        EpisodiveIconButton(
            onClick = { },
            icon = {
                Icon(
                    imageVector = EpisodiveIcons.Add,
                    contentDescription = null,
                )
            }
        )
    }
}

@ThemePreviews
@Composable
private fun EpisodiveIconProgressButtonPreview() {
    EpisodiveTheme {
        EpisodiveIconProgressButton(
            onClick = { },
            progress = 0.5f,
        ) {
            Icon(
                imageVector = EpisodiveIcons.Play,
                contentDescription = null,
            )
        }
    }
}

object EpisodiveIconButtonDefaults {
    const val DISABLED_ICON_BUTTON_CONTAINER_ALPHA = 0.12f
}