package io.jacob.episodive.core.designsystem.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import io.jacob.episodive.core.designsystem.icon.EpisodiveIcons
import io.jacob.episodive.core.designsystem.theme.EpisodiveTheme
import io.jacob.episodive.core.designsystem.tooling.DevicePreviews

@Composable
fun SectionHeader(
    modifier: Modifier = Modifier,
    title: String,
    actionIcon: ImageVector? = null,
    actionIconContentDescription: String? = null,
    onActionClick: () -> Unit = {},
    contentPadding: PaddingValues = PaddingValues(0.dp),
    content: @Composable ColumnScope.() -> Unit = {}
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                modifier = Modifier.weight(1f),
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface
            )

            if (actionIcon != null && actionIconContentDescription != null) {
                IconButton(
                    onClick = onActionClick
                ) {
                    Icon(
                        imageVector = actionIcon,
                        tint = MaterialTheme.colorScheme.onSurface,
                        contentDescription = actionIconContentDescription
                    )
                }
            }
        }

        Column(
            modifier = Modifier.padding(contentPadding)
        ) {
            content()
        }
    }
}

@Composable
fun SubSectionHeader(
    modifier: Modifier = Modifier,
    title: String,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    content: @Composable ColumnScope.() -> Unit = {}
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
    ) {
        Text(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Column(
            modifier = Modifier.padding(contentPadding)
        ) {
            content()
        }
    }
}

@Composable
fun FadeTopBarLayout(
    modifier: Modifier = Modifier,
    state: LazyListState,
    offset: Int = 700,
    title: String,
    onBack: () -> Unit,
    content: @Composable () -> Unit
) {
    val showTopBar by remember {
        derivedStateOf {
            val firstVisibleItem = state.firstVisibleItemIndex > 0
            val offsetPastFirst = state.firstVisibleItemIndex == 0 &&
                    state.firstVisibleItemScrollOffset > offset
            firstVisibleItem || offsetPastFirst
        }
    }

    Box(modifier = modifier) {
        content()

        EpisodiveTopAppBar(
            modifier = Modifier,
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = if (showTopBar) 1f else 0f)
            ),
            title = {
                AnimatedVisibility(
                    visible = showTopBar,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Text(
                        text = title,
                    )
                }
            },
            navigationIcon = EpisodiveIcons.ArrowBack,
            navigationIconContentDescription = "Back",
            onNavigationClick = onBack,
        )
    }
}

@DevicePreviews
@Composable
private fun SectionHeaderPreview() {
    EpisodiveTheme {
        SectionHeader(
            title = "Preview",
            actionIcon = EpisodiveIcons.KeyboardArrowRight,
            actionIconContentDescription = "See All",
            onActionClick = {}
        ) {
            Text(
                text = "Content",
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@DevicePreviews
@Composable
private fun SubSectionHeaderPreview() {
    EpisodiveTheme {
        SubSectionHeader(
            title = "Preview",
        ) {
            Text(
                text = "Content",
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@DevicePreviews
@Composable
private fun FadeTopBarLayoutPreview() {
    EpisodiveTheme {
        FadeTopBarLayout(
            state = rememberLazyListState(),
            title = "Title",
            onBack = {},
        ) {}
    }
}