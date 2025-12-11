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
import androidx.paging.PagingData
import androidx.paging.compose.collectAsLazyPagingItems
import io.jacob.episodive.core.designsystem.screen.LoadingScreen
import io.jacob.episodive.core.designsystem.theme.EpisodiveTheme
import io.jacob.episodive.core.designsystem.tooling.DevicePreviews
import io.jacob.episodive.core.model.Episode
import io.jacob.episodive.core.model.Playback
import io.jacob.episodive.core.model.Progress
import io.jacob.episodive.core.testing.model.episodeTestDataList
import io.jacob.episodive.core.ui.EpisodeClipItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.take
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
        viewModel.effect.collectLatest { effect ->
            when (effect) {
                is ClipEffect.NavigateToPodcast -> onPodcastClick(effect.podcastId)
            }
        }
    }

    val clipPlayerState by viewModel.clipPlayerState.collectAsStateWithLifecycle()

//    when (clipPlayerState.playback) {
//        Playback.IDLE -> LoadingScreen()
//        else ->
            ClipScreen(
                modifier = modifier,
                episodes = viewModel.episodes,
                playback = clipPlayerState.playback,
                progress = clipPlayerState.progress,
                isPlaying = clipPlayerState.isPlaying,
                onEpisodeChanged = { viewModel.sendAction(ClipAction.Play(it)) },
                onEpisodeClick = { viewModel.sendAction(ClipAction.ClickEpisode(it)) },
                onToggleEpisodeLiked = { viewModel.sendAction(ClipAction.ToggleEpisodeLiked(it)) },
                onPodcastClick = { viewModel.sendAction(ClipAction.ClickPodcast(it)) },
                onShowSnackbar = onShowSnackbar,
            )
//    }
}

@Composable
private fun ClipScreen(
    modifier: Modifier = Modifier,
    episodes: Flow<PagingData<Episode>>,
    playback: Playback,
    progress: Progress,
    isPlaying: Boolean,
    onEpisodeChanged: (Episode) -> Unit = {},
    onEpisodeClick: (Episode) -> Unit = {},
    onToggleEpisodeLiked: (Episode) -> Unit = {},
    onPodcastClick: (Long) -> Unit = {},
    onShowSnackbar: suspend (message: String, actionLabel: String?) -> Boolean = { _, _ -> false },
) {
    EpisodeClipPager(
        modifier = modifier,
        episodes = episodes,
        playback = playback,
        progress = progress,
        isPlaying = isPlaying,
        onEpisodeChanged = onEpisodeChanged,
        onEpisodeClick = onEpisodeClick,
        onToggleEpisodeLiked = onToggleEpisodeLiked,
        onPodcastClick = onPodcastClick,
    )

}

@Composable
fun EpisodeClipPager(
    modifier: Modifier = Modifier,
    episodes: Flow<PagingData<Episode>>,
    playback: Playback,
    progress: Progress,
    isPlaying: Boolean,
    onEpisodeChanged: (Episode) -> Unit = {},
    onEpisodeClick: (Episode) -> Unit = {},
    onToggleEpisodeLiked: (Episode) -> Unit = {},
    onPodcastClick: (Long) -> Unit = {},
) {
    val episodesPaging = episodes.collectAsLazyPagingItems()

    if (episodesPaging.itemCount == 0) {
        LoadingScreen()
        return
    }

    val pagerState = rememberPagerState(
        initialPage = 0,
        pageCount = { episodesPaging.itemCount }
    )

    // 첫 번째 에피소드 자동 재생 (최초 한 번만)
    LaunchedEffect(Unit) {
        snapshotFlow { episodesPaging.itemCount }
            .filter { it > 0 }
            .take(1)
            .collectLatest {
                episodesPaging[0]?.let { firstEpisode ->
                    onEpisodeChanged(firstEpisode)
                }
            }
    }

    // 페이지가 변경되면 해당 에피소드 재생
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }
            .distinctUntilChanged()
            .collectLatest { page ->
                if (page > 0) {
                    episodesPaging[page]?.let { episode ->
                        onEpisodeChanged(episode)
                    }
                }
            }
    }

    // 재생 완료 시 다음 페이지로 이동
    LaunchedEffect(playback) {
        if (playback == Playback.ENDED) {
            val nextPage = pagerState.currentPage + 1
            if (nextPage < episodesPaging.itemCount) {
                pagerState.animateScrollToPage(nextPage)
            }
        }
    }

    VerticalPager(
        state = pagerState,
        modifier = modifier.fillMaxSize(),
        key = { episodesPaging[it]?.id ?: it },
        pageSpacing = 32.dp, // 이전/다음 컨텐츠가 보이는 간격
        contentPadding = PaddingValues(vertical = 80.dp, horizontal = 24.dp) // 상하 여백으로 이전/다음 미리보기
    ) { page ->
        episodesPaging[page]?.let { episode ->
            EpisodeClipItem(
                modifier = Modifier.fillMaxSize(),
                episode = episode,
                isPlaying = isPlaying && page == pagerState.currentPage,
                remaining = progress.remaining,
                onClick = {
                    onEpisodeClick(episode)
                },
                onPlayEpisode = {
                    onEpisodeClick(episode)
                },
                onToggleEpisodeLiked = {
                    onToggleEpisodeLiked(episode)
                },
            )
        }
    }
}

@DevicePreviews
@Composable
private fun ClipScreenPreview() {
    EpisodiveTheme {
        ClipScreen(
            episodes = flowOf(
                PagingData.from(
                    episodeTestDataList.map {
                        it.copy(
                            clipStartTime = Instant.fromEpochMilliseconds(60_000L),
                            clipDuration = 1278.seconds,
                        )
                    }
                )),
            playback = Playback.READY,
            progress = Progress(
                position = 1000L.seconds,
                buffered = 1278.seconds,
                duration = 2000L.seconds,
            ),
            isPlaying = true,
        )
    }
}