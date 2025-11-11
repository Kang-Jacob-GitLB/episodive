package io.jacob.episodive.feature.clip

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.jacob.episodive.core.designsystem.component.EpisodeClipItem
import io.jacob.episodive.core.designsystem.screen.ErrorScreen
import io.jacob.episodive.core.designsystem.screen.LoadingScreen
import io.jacob.episodive.core.designsystem.theme.EpisodiveTheme
import io.jacob.episodive.core.designsystem.tooling.DevicePreviews
import io.jacob.episodive.core.model.Episode
import io.jacob.episodive.core.model.Progress
import io.jacob.episodive.core.testing.model.episodeTestDataList
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

@Composable
internal fun ClipRoute(
    modifier: Modifier = Modifier,
    viewModel: ClipViewModel = hiltViewModel(),
    onPodcastClick: (Long) -> Unit,
    onShowSnackbar: suspend (message: String, actionLabel: String?) -> Boolean,
) {
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> viewModel.sendAction(ClipAction.Resume)
                Lifecycle.Event.ON_STOP -> viewModel.sendAction(ClipAction.Pause)
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is ClipEffect.NavigateToPodcast -> onPodcastClick(effect.podcastId)
            }
        }
    }

    val state by viewModel.state.collectAsStateWithLifecycle()

    when (val s = state) {
        is ClipState.Loading -> LoadingScreen()
        is ClipState.Success -> {
            ClipScreen(
                modifier = modifier,
                episodes = s.episodes,
                indexOfPlaying = s.indexOfPlaying,
                progress = s.progress,
                isPlaying = s.isPlaying,
                onPageChanged = { viewModel.sendAction(ClipAction.PlayIndex(it)) },
                onEpisodeClick = { viewModel.sendAction(ClipAction.ClickEpisode(it)) },
                onToggleEpisodeLiked = { viewModel.sendAction(ClipAction.ToggleEpisodeLiked(it)) },
                onPodcastClick = { viewModel.sendAction(ClipAction.ClickPodcast(it)) },
                onShowSnackbar = onShowSnackbar,
            )
        }

        is ClipState.Error -> ErrorScreen(message = s.message)
    }
}

@Composable
private fun ClipScreen(
    modifier: Modifier = Modifier,
    episodes: List<Episode>,
    indexOfPlaying: Int = 0,
    progress: Progress,
    isPlaying: Boolean,
    onPageChanged: (Int) -> Unit = {},
    onEpisodeClick: (Episode) -> Unit = {},
    onToggleEpisodeLiked: (Episode) -> Unit = {},
    onPodcastClick: (Long) -> Unit = {},
    onShowSnackbar: suspend (message: String, actionLabel: String?) -> Boolean = { _, _ -> false },
) {
    EpisodeClipPager(
        modifier = modifier,
        episodes = episodes,
        indexOfPlaying = indexOfPlaying,
        progress = progress,
        isPlaying = isPlaying,
        onPageChanged = onPageChanged,
        onEpisodeClick = onEpisodeClick,
        onToggleEpisodeLiked = onToggleEpisodeLiked,
        onPodcastClick = onPodcastClick,
    )

}

@Composable
fun EpisodeClipPager(
    modifier: Modifier = Modifier,
    episodes: List<Episode>,
    indexOfPlaying: Int = 0,
    progress: Progress,
    isPlaying: Boolean,
    onPageChanged: (Int) -> Unit = {},
    onEpisodeClick: (Episode) -> Unit = {},
    onToggleEpisodeLiked: (Episode) -> Unit = {},
    onPodcastClick: (Long) -> Unit = {},
) {
    val pagerState = rememberPagerState(
        initialPage = indexOfPlaying,
        pageCount = { episodes.size }
    )

    // indexOfPlaying이 변경되면 pager를 해당 페이지로 이동
    LaunchedEffect(indexOfPlaying) {
        if (pagerState.currentPage != indexOfPlaying) {
            pagerState.animateScrollToPage(indexOfPlaying)
        }
    }

    // 사용자가 pager를 스와이프하면 player의 index 변경
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }
            .distinctUntilChanged()
            .collect { page ->
                if (page != indexOfPlaying) {
                    onPageChanged(page)
                }
            }
    }

    VerticalPager(
        state = pagerState,
        modifier = modifier.fillMaxSize(),
        key = { episodes[it].id },
        pageSpacing = 32.dp, // 이전/다음 컨텐츠가 보이는 간격
        contentPadding = PaddingValues(vertical = 80.dp, horizontal = 24.dp) // 상하 여백으로 이전/다음 미리보기
    ) { page ->
        EpisodeClipItem(
            modifier = Modifier.fillMaxSize(),
            episode = episodes[page],
            isPlaying = isPlaying && page == indexOfPlaying,
            remaining = progress.remaining,
            onClick = {
                onEpisodeClick(episodes[page])
            },
            onPlayEpisode = {
                onEpisodeClick(episodes[page])
            },
            onToggleEpisodeLiked = {
                onToggleEpisodeLiked(episodes[page])
            },
        )
    }
}

@DevicePreviews
@Composable
private fun ClipScreenPreview() {
    EpisodiveTheme {
        ClipScreen(
            episodes = episodeTestDataList.map {
                it.copy(
                    clipStartTime = Instant.fromEpochMilliseconds(60_000L),
                    clipDuration = 1278.seconds,
                )
            },
            indexOfPlaying = 0,
            progress = Progress(
                position = 1000L.seconds,
                buffered = 1278.seconds,
                duration = 2000L.seconds,
            ),
            isPlaying = true,
        )
    }
}