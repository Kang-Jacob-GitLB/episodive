package io.jacob.episodive.core.designsystem.component

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import io.jacob.episodive.core.designsystem.theme.EpisodiveTheme
import io.jacob.episodive.core.designsystem.tooling.ThemePreviews
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.IntOffset
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun EpisodiveSwipeDismissSnackbarHost(
    hostState: SnackbarHostState,
    modifier: Modifier = Modifier,
) {
    SnackbarHost(
        hostState = hostState,
        modifier = modifier,
    ) { snackbarData ->
        val offsetY = remember { Animatable(0f) }
        val scope = rememberCoroutineScope()

        Snackbar(
            snackbarData = snackbarData,
            modifier = Modifier
                .offset { IntOffset(0, offsetY.value.roundToInt()) }
                .draggable(
                    orientation = Orientation.Vertical,
                    state = rememberDraggableState { delta ->
                        scope.launch { offsetY.snapTo(offsetY.value + delta) }
                    },
                    onDragStopped = {
                        if (abs(offsetY.value) > DISMISS_THRESHOLD) {
                            snackbarData.dismiss()
                        } else {
                            scope.launch { offsetY.animateTo(0f) }
                        }
                    },
                ),
        )
    }
}

private const val DISMISS_THRESHOLD = 80f

@ThemePreviews
@Composable
private fun EpisodiveSwipeDismissSnackbarPreview() {
    EpisodiveTheme {
        val snackbarHostState = remember { SnackbarHostState() }
        LaunchedEffect(Unit) {
            snackbarHostState.showSnackbar(
                message = "팟캐스트를 구독했습니다",
                actionLabel = "실행 취소",
                duration = SnackbarDuration.Indefinite,
            )
        }
        EpisodiveSwipeDismissSnackbarHost(hostState = snackbarHostState)
    }
}
