package io.jacob.episodive.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
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
    size: Dp = EpisodiveIconButtonDefaults.CheckedIconButtonSize,
    colors: IconToggleButtonColors = IconButtonDefaults.iconToggleButtonColors(
        checkedContainerColor = MaterialTheme.colorScheme.primary,
        checkedContentColor = MaterialTheme.colorScheme.onPrimary,
        containerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
        contentColor = MaterialTheme.colorScheme.onSurface,
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
        modifier = modifier.size(size),
        enabled = enabled,
        shape = shape,
        colors = colors,
    ) {
        // 아이콘을 하나만 받았으면 갈래를 나누지 않는다. 같은 람다라도 if/else 의 다른 갈래에서
        // 부르면 상태가 바뀔 때마다 새로 붙어, 안에서 모양을 이어 바꾸는 아이콘이 움직이지 못한다.
        if (checkedIcon === icon) {
            icon()
        } else if (checked) {
            checkedIcon()
        } else {
            icon()
        }
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
        contentColor = MaterialTheme.colorScheme.onSurface,
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

/**
 * 진행률 링을 두른 원형 아이콘 버튼.
 *
 * [size] 는 링 바깥지름이다. 안쪽 원과 아이콘은 여기에 비례해 줄어들므로, 좁은 슬롯에
 * 놓을 때 [size] 만 줄이면 링이 슬롯 밖으로 삐져나가 잘리지 않는다. [modifier] 는 슬롯
 * 자체(예: `Modifier.weight(1f)`)에 적용되고 링은 그 안에서 가운데 정렬된다.
 */
@Composable
fun EpisodiveIconProgressButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    progress: Float = 0f,
    size: Dp = EpisodiveIconButtonDefaults.ProgressRingSize,
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
    val scale = size / EpisodiveIconButtonDefaults.ProgressRingSize
    val ringColor = if (enabled) colors.contentColor else colors.disabledContentColor
    val trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
    val strokeWidth = EpisodiveIconButtonDefaults.ProgressRingStrokeWidth * scale

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        IconButton(
            onClick = onClick,
            modifier = Modifier.size(size),
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
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.fillMaxSize(),
                        color = ringColor,
                        trackColor = trackColor,
                        strokeWidth = strokeWidth,
                    )
                } else {
                    CircularProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxSize(),
                        color = ringColor,
                        trackColor = trackColor,
                        strokeWidth = strokeWidth,
                        gapSize = 0.dp,
                    )
                }

                Box(
                    modifier = Modifier
                        .size(EpisodiveIconButtonDefaults.ProgressRingInnerSize * scale)
                        .clip(CircleShape)
                        .background(colors.containerColor),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        modifier = Modifier
                            .size(EpisodiveIconButtonDefaults.ProgressRingIconSize * scale)
                    ) {
                        icon()
                    }
                }
            }
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

    /** checked/unchecked 원형 아이콘 버튼 지름 */
    val CheckedIconButtonSize = 46.dp

    /** 진행률 링 원형 아이콘 버튼 지름 */
    val ProgressRingSize = 46.dp
    val ProgressRingStrokeWidth = 3.dp
    val ProgressRingInnerSize = 38.dp
    val ProgressRingIconSize = 17.dp
}