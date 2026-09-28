package io.jacob.episodive.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonColors
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarColors
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import io.jacob.episodive.core.designsystem.icon.EpisodiveIcons
import io.jacob.episodive.core.designsystem.theme.EpisodiveTheme
import io.jacob.episodive.core.designsystem.tooling.ThemePreviews

@Composable
fun EpisodiveTopAppBar(
    modifier: Modifier = Modifier,
    title: @Composable () -> Unit,
    navigationIcon: ImageVector? = null,
    navigationIconContentDescription: String? = null,
    actionIcon: ImageVector? = null,
    actionIconContentDescription: String? = null,
    secondaryActionIcon: ImageVector? = null,
    secondaryActionIconContentDescription: String? = null,
    colors: TopAppBarColors = TopAppBarDefaults.topAppBarColors(
        containerColor = Color.Transparent,
        scrolledContainerColor = Color.Transparent,
        navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
        titleContentColor = MaterialTheme.colorScheme.onSurface,
        actionIconContentColor = MaterialTheme.colorScheme.onSurface,
    ),
    navigationIconScrim: Color = EpisodiveTopAppBarDefaults.navigationIconScrim(),
    scrollBehavior: TopAppBarScrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(),
    onNavigationClick: () -> Unit = {},
    onActionClick: () -> Unit = {},
    onSecondaryActionClick: () -> Unit = {},
) {
    TopAppBar(
        title = {
            ProvideTextStyle(value = MaterialTheme.typography.titleMedium) {
                title()
            }
        },
        navigationIcon = {
            if (navigationIcon == null) return@TopAppBar
            if (navigationIconContentDescription == null) return@TopAppBar

            IconButton(
                onClick = onNavigationClick,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(navigationIconScrim),
            ) {
                Icon(
                    imageVector = navigationIcon,
                    contentDescription = navigationIconContentDescription,
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(20.dp),
                )
            }
        },
        actions = {
            // early-return 하나로 묶으면 primary(actionIcon)가 없는 화면에서 secondary 까지
            // 같이 사라진다. 그래서 두 아이콘을 독립된 if 블록으로 나눈다.
            // RowScope 는 좌→우로 쌓이므로, secondary 를 primary 왼쪽에 두려면 먼저 그린다.
            if (secondaryActionIcon != null && secondaryActionIconContentDescription != null) {
                IconButton(onClick = onSecondaryActionClick) {
                    Icon(
                        imageVector = secondaryActionIcon,
                        contentDescription = secondaryActionIconContentDescription,
                        tint = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }

            if (actionIcon != null && actionIconContentDescription != null) {
                IconButton(onClick = onActionClick) {
                    // 상태에 따라 아이콘을 바꿔 쓰는 자리(검색 ↔ 닫기)가 있다. 이어지는 모양이
                    // 아니라 돌려서 바꾼다.
                    RotateSwapIcon(
                        imageVector = actionIcon,
                        contentDescription = actionIconContentDescription,
                        tint = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        },
        colors = colors,
        scrollBehavior = scrollBehavior,
        modifier = modifier.testTag("episodiveTopAppBar"),
    )
}

@Composable
fun EpisodiveCenterTopAppBar(
    modifier: Modifier = Modifier,
    title: @Composable () -> Unit,
    navigationIcon: ImageVector? = null,
    navigationIconContentDescription: String? = null,
    actionIcon: ImageVector? = null,
    actionIconContentDescription: String? = null,
    colors: TopAppBarColors = TopAppBarDefaults.topAppBarColors(
        containerColor = Color.Transparent,
        scrolledContainerColor = Color.Transparent,
        navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
        titleContentColor = MaterialTheme.colorScheme.onSurface,
        actionIconContentColor = MaterialTheme.colorScheme.onSurface,
    ),
    iconButtonColors: IconButtonColors = IconButtonDefaults.iconButtonColors(),
    navigationIconScrim: Color = EpisodiveTopAppBarDefaults.navigationIconScrim(),
    windowInsets: WindowInsets = TopAppBarDefaults.windowInsets,
    scrollBehavior: TopAppBarScrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(),
    onNavigationClick: () -> Unit = {},
    onActionClick: () -> Unit = {},
) {
    CenterAlignedTopAppBar(
        title = {
            ProvideTextStyle(value = MaterialTheme.typography.titleMedium) {
                title()
            }
        },
        navigationIcon = {
            if (navigationIcon == null) return@CenterAlignedTopAppBar
            if (navigationIconContentDescription == null) return@CenterAlignedTopAppBar

            IconButton(
                onClick = onNavigationClick,
                colors = iconButtonColors,
                modifier = Modifier
                    // M3 가 주는 시작 여백은 4dp 뿐이라, 40dp 원형 배경이 화면 왼쪽 끝에
                    // 붙어 버린다. 원본은 left:16px 이므로 모자란 만큼 더한다 (원본 줄 322).
                    .padding(start = 12.dp)
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(navigationIconScrim),
            ) {
                Icon(
                    imageVector = navigationIcon,
                    contentDescription = navigationIconContentDescription,
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(20.dp),
                )
            }
        },
        actions = {
            if (actionIcon == null) return@CenterAlignedTopAppBar
            if (actionIconContentDescription == null) return@CenterAlignedTopAppBar

            IconButton(
                onClick = onActionClick,
                colors = iconButtonColors,
            ) {
                // 상태에 따라 아이콘을 바꿔 쓰는 자리(검색 ↔ 닫기)가 있다. 이어지는 모양이
                // 아니라 돌려서 바꾼다.
                RotateSwapIcon(
                    imageVector = actionIcon,
                    contentDescription = actionIconContentDescription,
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }
        },
        colors = colors,
        scrollBehavior = scrollBehavior,
        modifier = modifier.testTag("episodiveCenterTopAppBar"),
        windowInsets = windowInsets,
    )
}

@ThemePreviews
@Composable
private fun EpisodiveTopAppBarPreview() {
    EpisodiveTheme {
        EpisodiveTopAppBar(
            title = { Text(text = "Title") },
            navigationIcon = EpisodiveIcons.Search,
            navigationIconContentDescription = "Navigation icon",
            actionIcon = EpisodiveIcons.MoreVert,
            actionIconContentDescription = "Action icon",
            secondaryActionIcon = EpisodiveIcons.Transfer,
            secondaryActionIconContentDescription = "Secondary action icon",
        )
    }
}

@ThemePreviews
@Composable
private fun EpisodiveCenterTopAppBarPreview() {
    EpisodiveTheme {
        EpisodiveCenterTopAppBar(
            title = { Text(text = "Title") },
            navigationIcon = EpisodiveIcons.Search,
            navigationIconContentDescription = "Navigation icon",
            actionIcon = EpisodiveIcons.MoreVert,
            actionIconContentDescription = "Action icon",
        )
    }
}

object EpisodiveTopAppBarDefaults {
    /**
     * 배경이 비치는 탑바에서 네비게이션 아이콘 뒤에 까는 원.
     *
     * 검정 고정이면 라이트 테마에서 어두운 아이콘과 겹쳐 구분되지 않는다. 표면색을 쓰면
     * 다크에서는 어두운 원 + 밝은 아이콘, 라이트에서는 밝은 원 + 어두운 아이콘이 된다.
     */
    @Composable
    fun navigationIconScrim(): Color =
        MaterialTheme.colorScheme.surface.copy(alpha = 0.32f)
}
