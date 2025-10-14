package io.jacob.episodive.core.designsystem.screen

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import io.jacob.episodive.core.designsystem.component.LoadingWheel
import io.jacob.episodive.core.designsystem.theme.EpisodiveTheme
import io.jacob.episodive.core.designsystem.tooling.DevicePreviews

@Composable
fun LoadingScreen(
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        LoadingWheel()
    }
}

@DevicePreviews
@Composable
private fun LoadingScreenPreview() {
    EpisodiveTheme {
        LoadingScreen()
    }
}