package io.jacob.episodive.feature.soundbite

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
import io.jacob.episodive.core.model.ClipEpisode
import io.jacob.episodive.core.model.Episode
import io.jacob.episodive.core.model.Progress
import kotlinx.coroutines.flow.distinctUntilChanged

@Composable
internal fun SoundbiteRoute(
    modifier: Modifier = Modifier,
    viewModel: SoundbiteViewModel = hiltViewModel(),
    onPodcastClick: (Long) -> Unit,
    onShowSnackbar: suspend (message: String, actionLabel: String?) -> Boolean,
) {
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> viewModel.sendAction(SoundbiteAction.Resume)
                Lifecycle.Event.ON_STOP -> viewModel.sendAction(SoundbiteAction.Pause)
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
                is SoundbiteEffect.NavigateToPodcast -> onPodcastClick(effect.podcastId)
            }
        }
    }

    val state by viewModel.state.collectAsStateWithLifecycle()

    when (val s = state) {
        is SoundbiteState.Loading -> LoadingScreen()
        is SoundbiteState.Success -> {
            SoundbiteScreen(
                modifier = modifier,
                clipEpisodes = s.clipEpisodes,
                indexOfPlaying = s.indexOfPlaying,
                progress = s.progress,
                isPlaying = s.isPlaying,
                onPageChanged = { viewModel.sendAction(SoundbiteAction.PlayIndex(it)) },
                onEpisodeClick = { viewModel.sendAction(SoundbiteAction.ClickEpisode(it)) },
                onPodcastClick = { viewModel.sendAction(SoundbiteAction.ClickPodcast(it)) },
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
    indexOfPlaying: Int = 0,
    progress: Progress,
    isPlaying: Boolean,
    onPageChanged: (Int) -> Unit,
    onEpisodeClick: (Episode) -> Unit = {},
    onPodcastClick: (Long) -> Unit,
    onShowSnackbar: suspend (message: String, actionLabel: String?) -> Boolean,
) {
    EpisodeClipPager(
        modifier = modifier,
        clipEpisodes = clipEpisodes,
        indexOfPlaying = indexOfPlaying,
        progress = progress,
        isPlaying = isPlaying,
        onPageChanged = onPageChanged,
        onEpisodeClick = onEpisodeClick,
        onPodcastClick = onPodcastClick,
    )
}

@Composable
fun EpisodeClipPager(
    modifier: Modifier = Modifier,
    clipEpisodes: List<ClipEpisode>,
    indexOfPlaying: Int = 0,
    progress: Progress,
    isPlaying: Boolean,
    onPageChanged: (Int) -> Unit = {},
    onEpisodeClick: (Episode) -> Unit = {},
    onPodcastClick: (Long) -> Unit = {},
) {
    val pagerState = rememberPagerState(
        initialPage = indexOfPlaying,
        pageCount = { clipEpisodes.size }
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
        pageSpacing = 32.dp, // 이전/다음 컨텐츠가 보이는 간격
        contentPadding = PaddingValues(vertical = 80.dp, horizontal = 24.dp) // 상하 여백으로 이전/다음 미리보기
    ) { page ->
        EpisodeClipItem(
            modifier = Modifier.fillMaxSize(),
            clipEpisode = clipEpisodes[page],
            isPlaying = isPlaying && page == indexOfPlaying,
            remaining = progress.remaining,
            onClick = {
                onEpisodeClick(clipEpisodes[page].episode)
            },
        )
    }
}