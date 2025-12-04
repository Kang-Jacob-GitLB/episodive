package io.jacob.episodive.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import io.jacob.episodive.core.designsystem.icon.EpisodiveIcons
import io.jacob.episodive.core.designsystem.theme.EpisodiveTheme
import io.jacob.episodive.core.designsystem.tooling.ThemePreviews

@Composable
fun EpisodiveIconText(
    modifier: Modifier = Modifier,
    icon: @Composable () -> Unit,
    text: @Composable () -> Unit,
    iconLead: Boolean = true,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (iconLead) {
            icon()
            text()
        } else {
            text()
            icon()
        }
    }
}

@Composable
fun ClipAnimationIconText(
    modifier: Modifier = Modifier,
    barCount: Int = 5,
    color: Color = MaterialTheme.colorScheme.onSurface,
    isPlaying: Boolean,
    text: String,
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f),
        shape = CircleShape,
    ) {
        Row(
            modifier = modifier
                .padding(vertical = 8.dp, horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        ) {
            WaveAnimationIcon(
                modifier = Modifier.size(24.dp),
                barCount = barCount,
                color = color,
                isAnimating = isPlaying,
            )

            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}
@ThemePreviews
@Composable
private fun EpisodiveIconTextPreview() {
    EpisodiveTheme {
        EpisodiveIconText(
            icon = {
                Icon(
                    imageVector = EpisodiveIcons.PersonAdd,
                    contentDescription = null,
                )
            },
            text = {
                Text(
                    text = "12,3k",
                )
            }
        )
    }
}

@ThemePreviews
@Composable
private fun ClipAnimationIconTextPreview() {
    EpisodiveTheme {
        ClipAnimationIconText(
            text = "0:15",
            isPlaying = true,
        )
    }
}