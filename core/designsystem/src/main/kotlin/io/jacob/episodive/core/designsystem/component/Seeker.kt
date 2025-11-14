package io.jacob.episodive.core.designsystem.component

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import dev.vivvvek.seeker.Seeker
import dev.vivvvek.seeker.SeekerDefaults
import dev.vivvvek.seeker.Segment
import dev.vivvvek.seeker.rememberSeekerState
import io.jacob.episodive.core.designsystem.theme.EpisodiveTheme
import io.jacob.episodive.core.designsystem.tooling.ThemePreviews
import io.jacob.episodive.core.model.Chapter
import io.jacob.episodive.core.model.Progress
import io.jacob.episodive.core.model.mapper.toLongMillis
import kotlin.time.Duration.Companion.seconds

@Composable
fun EpisodiveSeeker(
    modifier: Modifier = Modifier,
    progress: Progress,
    onSeekTo: (Long) -> Unit,
    chapters: List<Chapter>,
    onChapterName: (String) -> Unit,
    isControllable: Boolean = true,
) {
    val state = rememberSeekerState()
    val interactionSource = remember { MutableInteractionSource() }
    val isDragging by interactionSource.collectIsDraggedAsState()

    val progressHeight = 4.dp
    val gap by animateDpAsState(if (isDragging) 4.dp else 2.dp)
    val thumbRadius by animateDpAsState(if (isDragging) 10.dp else 6.dp)

    var thumbPosition by remember { mutableStateOf(0f) }

    LaunchedEffect(state.currentSegment) {
        onChapterName(state.currentSegment.name)
    }

    Seeker(
        modifier = modifier
            .then(if (!isControllable) Modifier.height(progressHeight) else Modifier),
        state = state,
        value = progress.positionRatio,
        thumbValue = if (isDragging) thumbPosition else progress.positionRatio,
        readAheadValue = progress.bufferedRatio,
        range = 0f..1f,
        onValueChange = { thumbPosition = it },
        onValueChangeFinished = {
            onSeekTo((thumbPosition * progress.duration.toLongMillis()).toLong())
        },
        segments = chapters.map { chapter ->
            Segment(
                name = chapter.title,
                start = (chapter.startTime / progress.duration).toFloat(),
            )
        },
        enabled = isControllable,
        colors = SeekerDefaults.seekerColors(
            progressColor = MaterialTheme.colorScheme.primary,
            readAheadColor = MaterialTheme.colorScheme.primaryContainer,
            trackColor = MaterialTheme.colorScheme.outline,
            thumbColor = if (isControllable) MaterialTheme.colorScheme.primary else Color.Transparent,
            disabledProgressColor = MaterialTheme.colorScheme.primary,
            disabledTrackColor = MaterialTheme.colorScheme.outline,
            disabledThumbColor = if (isControllable) MaterialTheme.colorScheme.primary else Color.Transparent,
        ),
        dimensions = SeekerDefaults.seekerDimensions(
            trackHeight = progressHeight,
            thumbRadius = thumbRadius,
            gap = gap,
        ),
        interactionSource = interactionSource,
    )
}

@ThemePreviews
@Composable
private fun EpisodiveSeekerPreview() {
    EpisodiveTheme {
        EpisodiveSeeker(
            progress = Progress(
                position = 10000.seconds,
                buffered = 15000.seconds,
                duration = 20000.seconds,
            ),
            onSeekTo = {},
            chapters = listOf(
                Chapter(
                    title = "Chapter 1",
                    startTime = 0.seconds,
                    endTime = 5000.seconds,
                ),
                Chapter(
                    title = "Chapter 2",
                    startTime = 5000.seconds,
                    endTime = 15000.seconds,
                ),
                Chapter(
                    title = "Chapter 3",
                    startTime = 15000.seconds,
                    endTime = 20000.seconds,
                ),
            ),
            onChapterName = {},
        )
    }
}

@ThemePreviews
@Composable
private fun EpisodiveSeekerUncontrollablePreview() {
    EpisodiveTheme {
        EpisodiveSeeker(
            progress = Progress(
                position = 10000.seconds,
                buffered = 15000.seconds,
                duration = 20000.seconds,
            ),
            onSeekTo = {},
            chapters = listOf(
                Chapter(
                    title = "Chapter 1",
                    startTime = 0.seconds,
                    endTime = 5000.seconds,
                ),
                Chapter(
                    title = "Chapter 2",
                    startTime = 5000.seconds,
                    endTime = 15000.seconds,
                ),
                Chapter(
                    title = "Chapter 3",
                    startTime = 15000.seconds,
                    endTime = 20000.seconds,
                ),
            ),
            onChapterName = {},
            isControllable = false,
        )
    }
}