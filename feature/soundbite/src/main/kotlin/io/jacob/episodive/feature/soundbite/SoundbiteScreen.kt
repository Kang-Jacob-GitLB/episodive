package io.jacob.episodive.feature.soundbite

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.jacob.episodive.core.designsystem.component.EpisodeClipItem
import io.jacob.episodive.core.designsystem.screen.ErrorScreen
import io.jacob.episodive.core.designsystem.screen.LoadingScreen
import io.jacob.episodive.core.model.ClipEpisode

@Composable
internal fun SoundbiteRoute(
    modifier: Modifier = Modifier,
    viewModel: SoundbiteViewModel = hiltViewModel(),
    onShowSnackbar: suspend (message: String, actionLabel: String?) -> Boolean,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    when (val s = state) {
        is SoundbiteState.Loading -> LoadingScreen()
        is SoundbiteState.Success -> {
            SoundbiteScreen(
                modifier = modifier,
                clipEpisodes = s.clipEpisodes,
                onShowSnackbar = onShowSnackbar,
            )
        }

        is SoundbiteState.Error -> ErrorScreen(message = s.message)
    }
}

@Composable
private fun SoundbiteScreen(
    modifier: Modifier = Modifier,
    clipEpisodes: List<ClipEpisode>,
    onShowSnackbar: suspend (message: String, actionLabel: String?) -> Boolean,
) {
    EpisodeClipPager(
        modifier = modifier,
        clipEpisodes = clipEpisodes,
    )
}

@Composable
fun EpisodeClipPager(
    modifier: Modifier = Modifier,
    clipEpisodes: List<ClipEpisode>,
) {
    val pagerState = rememberPagerState(pageCount = { clipEpisodes.size })

    VerticalPager(
        state = pagerState,
        modifier = modifier.fillMaxSize(),
        pageSpacing = 32.dp, // 이전/다음 컨텐츠가 보이는 간격
        contentPadding = PaddingValues(vertical = 80.dp, horizontal = 24.dp) // 상하 여백으로 이전/다음 미리보기
    ) { page ->
        EpisodeClipItem(
            modifier = Modifier.fillMaxSize(),
            clipEpisode = clipEpisodes[page],
            isPlaying = page == pagerState.currentPage,
            onClick = {},
        )
    }
}