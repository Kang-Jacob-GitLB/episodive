package io.jacob.episodive.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
    val contentColor = if (expanded) {
        MaterialTheme.colorScheme.onSecondaryContainer
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    TextButton(
        onClick = { onExpandedChange(!expanded) },
        modifier = modifier,
        enabled = enabled,
        colors = ButtonDefaults.textButtonColors(
            contentColor = contentColor,
        ),
        contentPadding = contentPadding,
    ) {
        EpisodiveViewToggleContent(
            expanded = expanded,
            text = if (expanded) expandedText else compactText,
            trailingIcon = {
                MorphIcon(
                    imageVector = if (expanded) EpisodiveIcons.Collapse else EpisodiveIcons.Expand,
                    contentDescription = null,
                    tint = contentColor,
                )
            },
        )
    }
}

/**
 * [EpisodiveViewToggleButton] 과 같은 모습이되 **클릭을 받지 않는** 표시용 헤더.
 *
 * 카드나 행 전체가 이미 클릭 대상인 자리에 쓴다. 그런 곳에 버튼을 겹쳐 두면 클릭 지점이
 * 둘로 갈려 리플이 제목 주변에만 번지고, 스크린리더에도 같은 동작이 두 번 읽힌다.
 *
 * 높이는 버튼이 잡던 것과 같게 유지한다([EpisodiveViewToggleDefaults.HeaderMinHeight]).
 * 버튼만 걷어내면 최소 터치 크기 보정이 사라져 그만큼 헤더가 낮아지고, 같은 화면에서
 * 카드 높이가 통째로 줄어든다.
 */
@Composable
fun EpisodiveViewToggleHeader(
    modifier: Modifier = Modifier,
    expanded: Boolean,
    contentPadding: PaddingValues = EpisodiveViewToggleDefaults.ViewToggleButtonContentPadding,
    text: @Composable () -> Unit = {},
    compactText: @Composable () -> Unit = text,
    expandedText: @Composable () -> Unit = text,
) {
    val contentColor = if (expanded) {
        MaterialTheme.colorScheme.onSecondaryContainer
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    // 버튼이 하던 색 제공을 대신한다. 없으면 제목이 주변 색을 그대로 받아, 버튼일 때와
    // 다른 색으로 보인다.
    CompositionLocalProvider(LocalContentColor provides contentColor) {
        EpisodiveViewToggleContent(
            modifier = modifier
                .heightIn(min = EpisodiveViewToggleDefaults.HeaderMinHeight)
                .padding(contentPadding),
            expanded = expanded,
            text = if (expanded) expandedText else compactText,
            trailingIcon = {
                MorphIcon(
                    imageVector = if (expanded) EpisodiveIcons.Collapse else EpisodiveIcons.Expand,
                    contentDescription = null,
                    tint = contentColor,
                )
            },
        )
    }
}

@Composable
private fun EpisodiveViewToggleContent(
    modifier: Modifier = Modifier,
    expanded: Boolean,
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
                        color = if (expanded) {
                            MaterialTheme.colorScheme.secondaryContainer
                        } else {
                            Color.Transparent
                        },
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

    /**
     * [EpisodiveViewToggleHeader] 의 최소 높이.
     *
     * M3 가 버튼에 붙이는 최소 터치 크기(48dp)와 같다. 버튼을 걷어낸 자리에 이 값을 두지
     * 않으면 헤더가 그만큼 낮아져 카드 높이가 통째로 달라진다.
     */
    val HeaderMinHeight = 48.dp
}