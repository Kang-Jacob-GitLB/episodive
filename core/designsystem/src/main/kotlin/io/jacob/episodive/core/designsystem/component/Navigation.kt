package io.jacob.episodive.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import io.jacob.episodive.core.designsystem.icon.EpisodiveIcons
import io.jacob.episodive.core.designsystem.theme.EpisodiveTheme
import io.jacob.episodive.core.designsystem.theme.LocalDimensionTheme
import io.jacob.episodive.core.designsystem.tooling.DevicePreviews

@Composable
fun RowScope.EpisodiveNavigationBarItem(
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    alwaysShowLabel: Boolean = true,
    icon: @Composable () -> Unit,
    label: @Composable (() -> Unit)? = null,
) {
    val dimension = LocalDimensionTheme.current
    val contentColor = if (selected) {
        EpisodiveNavigationDefaults.navigationSelectedItemColor()
    } else {
        EpisodiveNavigationDefaults.navigationContentColor()
    }

    Column(
        modifier = modifier
            .weight(1f)
            .selectable(
                selected = selected,
                onClick = onClick,
                enabled = enabled,
                role = Role.Tab,
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        // 활성: 56x30 pill(반경 16) + secondaryContainer 배경. 비활성: 배경 없이 동일 크기 영역만 유지.
        Box(
            modifier = Modifier
                .size(width = dimension.navIndicatorWidth, height = dimension.navIndicatorHeight)
                .then(
                    if (selected) {
                        Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(EpisodiveNavigationDefaults.navigationIndicatorColor())
                    } else {
                        Modifier
                    }
                ),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier.size(22.dp),
                contentAlignment = Alignment.Center,
            ) {
                CompositionLocalProvider(LocalContentColor provides contentColor) {
                    // 선택용 아이콘을 따로 받지 않는다. 갈래를 나눠 부르면 선택이 바뀔 때마다
                    // 아이콘이 새로 붙어, 모양을 이어 바꾸는 아이콘(MorphIcon)이 움직이지 못한다.
                    icon()
                }
            }
        }

        if (label != null && (alwaysShowLabel || selected)) {
            CompositionLocalProvider(LocalContentColor provides contentColor) {
                label()
            }
        }
    }
}

@Composable
fun EpisodiveNavigationBar(
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit,
) {
    val dimension = LocalDimensionTheme.current
    val borderColor = MaterialTheme.colorScheme.outlineVariant

    // NavigationBar 는 시스템 제스처 인셋만큼 내부 패딩을 넣는다. 높이를 74dp 로 고정하면
    // 그 인셋이 74dp 안을 파먹어 실제 콘텐츠가 50dp 로 눌린다. 인셋만큼 높이를 늘려
    // 콘텐츠 영역이 디자인의 74dp 가 되게 하고, 배경은 인셋 영역까지 칠해지도록 둔다.
    val bottomInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    NavigationBar(
        modifier = modifier
            .height(dimension.navigationBarHeight + bottomInset)
            .drawBehind {
                drawLine(
                    color = borderColor,
                    start = Offset(0f, 0f),
                    end = Offset(size.width, 0f),
                    strokeWidth = 1.dp.toPx(),
                )
            },
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        contentColor = EpisodiveNavigationDefaults.navigationContentColor(),
        tonalElevation = 0.dp,
        content = content,
    )
}

@DevicePreviews
@Composable
private fun EpisodiveNavigationBarPreview() {
    val items = listOf("Home", "Search", "Library", "Clip")
    val selectedIcons = listOf(
        EpisodiveIcons.HomeFilled,
        EpisodiveIcons.SearchFilled,
        EpisodiveIcons.LibraryFilled,
        EpisodiveIcons.ClipFilled,
    )
    val unselectedIcon = listOf(
        EpisodiveIcons.Home,
        EpisodiveIcons.Search,
        EpisodiveIcons.Library,
        EpisodiveIcons.Clip,
    )

    EpisodiveTheme {
        EpisodiveNavigationBar {
            items.forEachIndexed { index, item ->
                EpisodiveNavigationBarItem(
                    icon = {
                        MorphIcon(
                            imageVector = if (index == 0) selectedIcons[index] else unselectedIcon[index],
                            contentDescription = null
                        )
                    },
                    label = {
                        Text(
                            text = item,
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 1
                        )
                    },
                    selected = index == 0,
                    onClick = { },
                )
            }
        }
    }
}

object EpisodiveNavigationDefaults {
    @Composable
    fun navigationContentColor() = MaterialTheme.colorScheme.onSurfaceVariant

    @Composable
    fun navigationSelectedItemColor() = MaterialTheme.colorScheme.onSecondaryContainer

    @Composable
    fun navigationIndicatorColor() = MaterialTheme.colorScheme.secondaryContainer
}
