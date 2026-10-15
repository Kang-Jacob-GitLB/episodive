package io.jacob.episodive.core.ui.caption

import androidx.compose.foundation.layout.size
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.jacob.episodive.core.designsystem.component.EpisodiveIconButton
import io.jacob.episodive.core.designsystem.component.EpisodiveIconProgressButton
import io.jacob.episodive.core.designsystem.icon.EpisodiveIcons
import io.jacob.episodive.core.ui.R

/** 다운로드 링이 아이콘 버튼을 감싸는 지름. */
private val CaptionRingSize = 40.dp

/**
 * 컨트롤 바 자막 토글. [buttonState] 는 람다로만 읽는다 — [CaptionButtonState] 는 자막 텍스트가
 * 아니라 켜짐 여부·가용성에만 좌우되지만, 같은 원본 `LiveCaptionState` 를 구독하는 이상 매
 * partial 갱신마다 `.value` 읽기 자체가 재구성을 유발한다. 그 재구성을 이 버튼 하나로 가둔다.
 */
@Composable
fun CaptionToggleButton(
    modifier: Modifier = Modifier,
    buttonState: () -> CaptionButtonState,
    onClick: () -> Unit,
) {
    val turnOnDescription = stringResource(R.string.core_ui_caption_turn_on)
    val turnOffDescription = stringResource(R.string.core_ui_caption_turn_off)
    val downloadingDescription = stringResource(R.string.core_ui_caption_downloading)

    when (val state = buttonState()) {
        is CaptionButtonState.Progress -> EpisodiveIconProgressButton(
            modifier = modifier,
            onClick = onClick,
            size = CaptionRingSize,
            isLoading = state.progress <= 0f,
            progress = state.progress,
            colors = IconButtonDefaults.iconButtonColors(
                containerColor = Color.Transparent,
                contentColor = MaterialTheme.colorScheme.primary,
            ),
            icon = {
                Icon(
                    modifier = Modifier.size(21.dp),
                    imageVector = EpisodiveIcons.Caption,
                    contentDescription = downloadingDescription,
                )
            },
        )

        CaptionButtonState.Active -> EpisodiveIconButton(
            modifier = modifier,
            onClick = onClick,
            icon = {
                Icon(
                    modifier = Modifier.size(21.dp),
                    imageVector = EpisodiveIcons.Caption,
                    contentDescription = turnOffDescription,
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        )

        CaptionButtonState.Dimmed -> EpisodiveIconButton(
            modifier = modifier,
            onClick = onClick,
            icon = {
                Icon(
                    modifier = Modifier.size(21.dp),
                    imageVector = EpisodiveIcons.Caption,
                    contentDescription = turnOffDescription,
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.38f),
                )
            }
        )

        CaptionButtonState.Inactive -> EpisodiveIconButton(
            modifier = modifier,
            onClick = onClick,
            icon = {
                Icon(
                    modifier = Modifier.size(21.dp),
                    imageVector = EpisodiveIcons.Caption,
                    contentDescription = turnOnDescription,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        )
    }
}
