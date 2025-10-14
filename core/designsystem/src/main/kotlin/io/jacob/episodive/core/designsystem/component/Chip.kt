package io.jacob.episodive.core.designsystem.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import io.jacob.episodive.core.designsystem.theme.EpisodiveTheme
import io.jacob.episodive.core.designsystem.tooling.ThemePreviews

@Composable
fun EpisodiveFilterChip(
    selected: Boolean,
    onSelectedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = RoundedCornerShape(8.dp),
    label: @Composable () -> Unit,
) {
    val containerColor by animateColorAsState(
        targetValue = when {
            !enabled && selected -> MaterialTheme.colorScheme.onBackground.copy(
                alpha = EpisodiveChipDefaults.DISABLED_CHIP_CONTAINER_ALPHA,
            )

            !enabled -> Color.Transparent
            selected -> MaterialTheme.colorScheme.primaryContainer
            else -> MaterialTheme.colorScheme.surfaceVariant
        },
        label = "chipContainerColor"
    )

    FilterChip(
        selected = selected,
        onClick = { onSelectedChange(!selected) },
        label = {
            ProvideTextStyle(value = MaterialTheme.typography.labelSmall) {
                label()
            }
        },
        modifier = modifier,
        enabled = enabled,
        shape = shape,
        colors = FilterChipDefaults.filterChipColors(
            containerColor = containerColor,
            selectedContainerColor = containerColor,
            labelColor = MaterialTheme.colorScheme.onBackground,
            iconColor = MaterialTheme.colorScheme.onBackground,
            disabledContainerColor = containerColor,
            disabledLabelColor = MaterialTheme.colorScheme.onBackground.copy(
                alpha = EpisodiveChipDefaults.DISABLED_CHIP_CONTENT_ALPHA,
            ),
            disabledLeadingIconColor = MaterialTheme.colorScheme.onBackground.copy(
                alpha = EpisodiveChipDefaults.DISABLED_CHIP_CONTENT_ALPHA,
            ),
            selectedLabelColor = MaterialTheme.colorScheme.onBackground,
            selectedLeadingIconColor = MaterialTheme.colorScheme.onBackground,
        ),
    )
}

@ThemePreviews
@Composable
private fun ChipPreview() {
    EpisodiveTheme {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            EpisodiveFilterChip(selected = true, onSelectedChange = {}) {
                Text("selected")
            }
            EpisodiveFilterChip(selected = false, onSelectedChange = {}) {
                Text("unselected")
            }
        }
    }
}

object EpisodiveChipDefaults {
    const val DISABLED_CHIP_CONTAINER_ALPHA = 0.12f
    const val DISABLED_CHIP_CONTENT_ALPHA = 0.38f
    val ChipBorderWidth = 1.dp
}