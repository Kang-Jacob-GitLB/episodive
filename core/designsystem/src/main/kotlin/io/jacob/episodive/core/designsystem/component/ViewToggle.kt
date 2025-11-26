package io.jacob.episodive.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.jacob.episodive.core.designsystem.icon.EpisodiveIcons
import io.jacob.episodive.core.designsystem.theme.EpisodiveTheme
import io.jacob.episodive.core.designsystem.tooling.ThemePreviews

@Composable
fun EpisodiveViewToggleButton(
    modifier: Modifier = Modifier,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    enabled: Boolean = true,
    contentPadding: PaddingValues = EpisodiveViewToggleDefaults.ViewToggleButtonContentPadding,
    text: @Composable () -> Unit = {},
    compactText: @Composable () -> Unit = text,
    expandedText: @Composable () -> Unit = text,
) {
    TextButton(
        onClick = { onExpandedChange(!expanded) },
        modifier = modifier,
        enabled = enabled,
        colors = ButtonDefaults.textButtonColors(
            contentColor = MaterialTheme.colorScheme.onBackground,
        ),
        contentPadding = contentPadding,
    ) {
        EpisodiveViewToggleContent(
            text = if (expanded) expandedText else compactText,
            trailingIcon = {
                Icon(
                    imageVector = if (expanded) EpisodiveIcons.Collapse else EpisodiveIcons.Expand,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onBackground,
                )
            },
        )
    }
}

@Composable
private fun EpisodiveViewToggleContent(
    modifier: Modifier = Modifier,
    text: @Composable () -> Unit,
    trailingIcon: @Composable (() -> Unit)? = null,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        ProvideTextStyle(value = MaterialTheme.typography.titleLarge) {
            text()
        }

        if (trailingIcon != null) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .background(
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        shape = CircleShape,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                trailingIcon()
            }
        }
    }
}

@ThemePreviews
@Composable
private fun ViewTogglePreviewExpanded() {
    EpisodiveTheme {
        Surface {
            EpisodiveViewToggleButton(
                expanded = true,
                onExpandedChange = { },
                compactText = { Text(text = "Compact view") },
                expandedText = { Text(text = "Expanded view") },
            )
        }
    }
}

@ThemePreviews
@Composable
private fun ViewTogglePreviewCompact() {
    EpisodiveTheme {
        Surface {
            EpisodiveViewToggleButton(
                expanded = false,
                onExpandedChange = { },
                compactText = { Text(text = "Compact view") },
                expandedText = { Text(text = "Expanded view") },
            )
        }
    }
}

object EpisodiveViewToggleDefaults {
    val ViewToggleButtonContentPadding =
        PaddingValues(
            start = 16.dp,
            top = 8.dp,
            end = 12.dp,
            bottom = 8.dp,
        )
}