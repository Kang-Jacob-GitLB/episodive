package io.jacob.episodive.core.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.jacob.episodive.core.designsystem.icon.EpisodiveIcons
import io.jacob.episodive.core.designsystem.theme.EpisodiveTheme
import io.jacob.episodive.core.designsystem.tooling.DevicePreviews
import io.jacob.episodive.core.model.Chapter
import io.jacob.episodive.core.model.mapper.toMediaTime
import kotlin.time.Duration.Companion.seconds

@Composable
fun ChapterItem(
    modifier: Modifier = Modifier,
    chapter: Chapter,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() },
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = EpisodiveIcons.LetterI,
                contentDescription = "Chapter Indicator",
                tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Text(
                modifier = Modifier
                    .padding(end = 8.dp)
                    .weight(1f),
                text = chapter.title,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            Text(
                text = chapter.startTime.toMediaTime(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
            )
        }

        HorizontalDivider(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 24.dp),
            thickness = 1.dp,
            color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
        )
    }
}

@DevicePreviews
@Composable
private fun ChapterItemPreview() {
    EpisodiveTheme {
        ChapterItem(
            chapter = Chapter(
                title = "Chapter 1 unselected",
                startTime = 1000.seconds,
                endTime = 2000.seconds
            ),
            isSelected = false,
            onClick = {}
        )
    }
}

@DevicePreviews
@Composable
private fun ChapterItemSelectedPreview() {
    EpisodiveTheme {
        ChapterItem(
            chapter = Chapter(
                title = "Chapter 2 selected",
                startTime = 1000.seconds,
                endTime = 2000.seconds
            ),
            isSelected = true,
            onClick = {}
        )
    }
}