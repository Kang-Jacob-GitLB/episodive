package io.jacob.episodive.core.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import io.jacob.episodive.core.designsystem.component.EpisodiveDragHandle
import io.jacob.episodive.core.designsystem.component.EpisodiveSwipeDismissSnackbarHost
import io.jacob.episodive.core.designsystem.icon.EpisodiveIcons
import io.jacob.episodive.core.designsystem.theme.EpisodiveShapes
import io.jacob.episodive.core.model.SLEEP_TIMER_FADE_OUT_MS
import kotlinx.coroutines.launch

/** v2: 슬립 타이머 남은 시간 숫자 — 타이포 스케일에 없는 일회성 크기(원본 줄 413). */
private val SleepTimerValueTextStyle = TextStyle(fontSize = 40.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.02.em)

/**
 * 슬립 타이머 프리셋 원 지름. 목업 폭(344 - 좌우 22)에 다섯 개가 8dp 간격으로 한 줄에
 * 들어가는 크기다. 이보다 크면 좁은 기기에서 줄이 넘어간다.
 */
private val SleepTimerPresetSize = 52.dp

/**
 * 슬립 타이머 시트. 에피소드·라디오 플레이어가 함께 쓴다. 라이브에는 "끝"이 없으므로 라디오는
 * [showEndOfEpisode] 를 꺼서 "에피소드 끝까지"를 숨긴다.
 */
@Composable
fun SleepTimerSheet(
    modifier: Modifier = Modifier,
    remainingMs: Long?,
    isPlaying: Boolean,
    showEndOfEpisode: Boolean = true,
    onSetTimer: (Long) -> Unit = {},
    onEndOfEpisode: () -> Unit = {},
    onCancel: () -> Unit = {},
    onDismiss: () -> Unit = {},
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val requiresPlaybackMessage = stringResource(R.string.core_ui_sleep_timer_requires_playback)

    fun handleAction(action: () -> Unit) {
        if (isPlaying) {
            action()
        } else {
            scope.launch {
                snackbarHostState.currentSnackbarData?.dismiss()
                snackbarHostState.showSnackbar(
                    message = requiresPlaybackMessage,
                    duration = SnackbarDuration.Short,
                )
            }
        }
    }

    // 한 줄에 들어가는 다섯 개까지만 둔다. 줄바꿈되면 시트가 세로로 늘어나 다이얼·버튼이 밀린다.
    // 커스텀 시간 입력이 없어 이 다섯 개가 선택지의 전부이므로, 칸을 잠들지 않을 시간에 쓰지 않는다.
    val timerPresets = remember {
        listOf(
            5L * 60 * 1000,
            10L * 60 * 1000,
            15L * 60 * 1000,
            30L * 60 * 1000,
            60L * 60 * 1000,
        )
    }
    val isActive = remainingMs != null

    ModalBottomSheet(
        modifier = modifier.fillMaxWidth(),
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        shape = EpisodiveShapes.bottomSheet,
        dragHandle = { EpisodiveDragHandle() }
    ) {
        Box {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .animateContentSize()
                    .padding(horizontal = 22.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = stringResource(R.string.core_ui_sleep_timer),
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                val displayMs = remainingMs ?: 0L
                val minutes = displayMs / 1000 / 60
                val seconds = (displayMs / 1000) % 60
                val timerColor = when {
                    !isActive -> MaterialTheme.colorScheme.onSurface
                    displayMs <= SLEEP_TIMER_FADE_OUT_MS -> {
                        val fraction = displayMs / SLEEP_TIMER_FADE_OUT_MS.toFloat()
                        lerp(
                            MaterialTheme.colorScheme.onSurface,
                            MaterialTheme.colorScheme.primary,
                            fraction,
                        )
                    }
                    else -> MaterialTheme.colorScheme.primary
                }
                val moonColor = if (isActive) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        modifier = Modifier.size(26.dp),
                        imageVector = EpisodiveIcons.Moon,
                        contentDescription = null,
                        tint = moonColor,
                    )

                    Text(
                        text = String.format("%d:%02d", minutes, seconds),
                        style = SleepTimerValueTextStyle,
                        color = timerColor,
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    timerPresets.forEach { durationMs ->
                        // 단위는 로케일 리소스가 붙인다 (한국어 "분", 영어 "m").
                        val label = stringResource(
                            R.string.core_ui_sleep_timer_minutes,
                            (durationMs / 1000 / 60).toInt(),
                        )
                        PresetCircle(
                            size = SleepTimerPresetSize,
                            label = label,
                            selected = false,
                            onClick = { handleAction { onSetTimer(durationMs) } },
                        )
                    }
                }

                // 라이브 스테이션에는 끝이 없다.
                if (showEndOfEpisode) {
                    SheetActionButton(
                        onClick = { handleAction { onEndOfEpisode() } },
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        contentColor = MaterialTheme.colorScheme.onSurface,
                        icon = EpisodiveIcons.SkipNext,
                        text = stringResource(R.string.core_ui_sleep_timer_end_of_episode),
                    )
                }

                AnimatedVisibility(visible = isActive) {
                    SheetActionButton(
                        onClick = { onCancel() },
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer,
                        icon = EpisodiveIcons.Close,
                        text = stringResource(R.string.core_ui_sleep_timer_cancel),
                        fontWeight = FontWeight.Bold,
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
            }

            EpisodiveSwipeDismissSnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }
    }
}
