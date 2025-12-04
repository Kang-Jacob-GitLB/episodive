package io.jacob.episodive.core.ui

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.jacob.episodive.core.designsystem.component.EpisodiveIconToggleButton
import io.jacob.episodive.core.designsystem.component.StateImage
import io.jacob.episodive.core.designsystem.icon.EpisodiveIcons
import io.jacob.episodive.core.designsystem.theme.EpisodiveTheme
import io.jacob.episodive.core.designsystem.tooling.DevicePreviews
import io.jacob.episodive.core.model.Category

@Composable
fun CategoryButton(
    modifier: Modifier = Modifier,
    category: Category,
    isSelected: Boolean,
    onClick: (Category) -> Unit,
) {
    Surface(
        modifier = modifier
            .size(140.dp)
            .then(
                if (isSelected) {
                    Modifier.border(
                        width = 2.dp,
                        color = MaterialTheme.colorScheme.primary,
                        shape = MaterialTheme.shapes.extraLarge
                    )
                } else {
                    Modifier
                }
            ),
        shape = MaterialTheme.shapes.extraLarge,
        selected = isSelected,
        onClick = {
            onClick(category)
        },
    ) {
        val blurRadius by animateDpAsState(
            targetValue = if (isSelected) 8.dp else 0.dp,
            animationSpec = tween(300),
            label = "blurRadius"
        )

        StateImage(
            modifier = Modifier
                .fillMaxSize()
                .blur(blurRadius),
            imageUrl = category.imageUrl,
            contentDescription = category.label,
        )

        Box(
            modifier = Modifier.padding(12.dp),
        ) {
            val textSize by animateFloatAsState(
                targetValue = if (isSelected) 22f else 16f,
                animationSpec = tween(300),
                label = "textSize"
            )

            val offsetY by animateDpAsState(
                targetValue = if (isSelected) 0.dp else 45.dp,
                animationSpec = tween(300),
                label = "offsetY"
            )

            Text(
                text = category.label,
                fontSize = textSize.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset(y = offsetY),
                color = MaterialTheme.colorScheme.onSurface,
            )

            EpisodiveIconToggleButton(
                modifier = Modifier
                    .size(24.dp)
                    .align(Alignment.TopEnd),
                checked = isSelected,
                onCheckedChange = { checked -> onClick(category) },
                icon = {
                    Icon(
                        modifier = Modifier
                            .size(16.dp),
                        imageVector = EpisodiveIcons.Add,
                        contentDescription = category.label,
                    )
                },
                checkedIcon = {
                    Icon(
                        modifier = Modifier
                            .size(16.dp),
                        imageVector = EpisodiveIcons.Check,
                        contentDescription = category.label,
                    )
                },
            )
        }
    }
}

@Composable
fun CategoryItem(
    modifier: Modifier = Modifier,
    category: Category,
    onClick: () -> Unit,
) {
    Column(
        modifier = modifier
            .clickable { onClick() },
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        StateImage(
            modifier = Modifier
                .size(140.dp)
                .clip(MaterialTheme.shapes.largeIncreased),
            imageUrl = category.imageUrl,
            contentDescription = category.name,
        )

        Text(
            text = category.label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@DevicePreviews
@Composable
private fun CategoryButtonPreview() {
    EpisodiveTheme {
        Column {
            CategoryButton(
                category = Category.ENTREPRENEURSHIP,
                isSelected = true,
                onClick = {},
            )
            CategoryButton(
                category = Category.ENTREPRENEURSHIP,
                isSelected = false,
                onClick = {},
            )
        }
    }
}

@DevicePreviews
@Composable
private fun CategoryItemPreview() {
    EpisodiveTheme {
        CategoryItem(
            category = Category.ENTREPRENEURSHIP,
            onClick = {},
        )
    }
}
