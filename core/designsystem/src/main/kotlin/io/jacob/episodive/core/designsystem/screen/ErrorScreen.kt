package io.jacob.episodive.core.designsystem.screen

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import io.jacob.episodive.core.designsystem.theme.EpisodiveTheme
import io.jacob.episodive.core.designsystem.tooling.DevicePreviews

@Composable
fun ErrorScreen(
    modifier: Modifier = Modifier,
    message: String,
) {
    Box(
        modifier = modifier
            .fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(text = message)
    }
}

@DevicePreviews
@Composable
private fun ErrorScreenPreview() {
    EpisodiveTheme {
        ErrorScreen(message = "An error occurred")
    }
}