package io.jacob.episodive.feature.player

import android.text.format.Formatter
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.jacob.episodive.core.designsystem.component.EpisodiveIconToggleButton
import io.jacob.episodive.core.designsystem.component.FadingEdgeText
import io.jacob.episodive.core.designsystem.component.StateImage
import io.jacob.episodive.core.designsystem.icon.EpisodiveIcons
import io.jacob.episodive.core.designsystem.theme.EpisodiveShapes
import io.jacob.episodive.core.designsystem.theme.surfaceContainerHighDark
import io.jacob.episodive.core.designsystem.theme.EpisodiveTheme
import io.jacob.episodive.core.designsystem.theme.LocalDimensionTheme
import io.jacob.episodive.core.designsystem.tooling.DevicePreviews
import io.jacob.episodive.core.model.Chapter
import io.jacob.episodive.core.model.Episode
import io.jacob.episodive.core.model.Podcast
import io.jacob.episodive.core.model.Progress
import io.jacob.episodive.core.model.caption.CaptionDownloadFailure
import io.jacob.episodive.core.model.coverUrl
import io.jacob.episodive.core.testing.model.episodeTestData
import io.jacob.episodive.core.testing.model.podcastTestData
import io.jacob.episodive.core.model.share.EpisodiveDeepLink
import io.jacob.episodive.core.ui.R as uiR
import kotlin.time.Duration.Companion.seconds

/**
 * 미니플레이어 틴트의 바탕이 되는 표면색. 전경(제목·부제·좋아요·진행바 트랙)이 전부
 * 흰색이라 라이트/다크 어느 테마에서도 어두워야 한다 — 다크 스킴 값으로 고정한다.
 */
private val PlayerBarTintSurface = surfaceContainerHighDark

/** 커버 추출색을 미니플레이어 표면색 쪽으로 섞는 비율 (0 = 추출색 그대로). */
private const val PlayerBarTintStartBlend = 0.45f
private const val PlayerBarTintEndBlend = 0.9f

@Composable
fun PlayerBar(
    modifier: Modifier = Modifier,
    viewModel: PlayerViewModel = hiltViewModel(),
    onPodcastClick: (Long) -> Unit,
    onShowSnackbar: suspend (message: String, actionLabel: String?) -> Boolean = { _, _ -> false },
    expandSignal: Int = 0,
    collapseSignal: Int = 0,
    pendingEpisode: () -> EpisodiveDeepLink.Episode? = { null },
    onPendingEpisodeHandled: () -> Unit = {},
    onNowPlayingChange: (Long?) -> Unit = {},
    onIsPlayingChange: (Boolean) -> Unit = {},
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    var isShowPlayer by remember { mutableStateOf(false) }

    // 목록 화면이 "이 항목이 재생 중인가"를 알 수 있도록 현재 에피소드 id 를 위로 올린다.
    val nowPlayingId = (state as? PlayerState.Success)?.nowPlaying?.id
    LaunchedEffect(nowPlayingId) {
        onNowPlayingChange(nowPlayingId)
    }

    // 위 id 만으로는 일시정지를 구분하지 못한다. 재생/일시정지를 갈라야 하는 화면을 위해 함께 올린다.
    val isPlaying = (state as? PlayerState.Success)?.isPlaying == true
    LaunchedEffect(isPlaying) {
        onIsPlayingChange(isPlaying)
    }

    // 외부(위젯 now-playing 딥링크)에서 시그널이 증가하면 플레이어 시트를 펼친다.
    // 일회성 effect 대신 상태 기반이라 콜드 스타트 타이밍에도 유실되지 않는다.
    // state 가 아직 Success 가 아니면 시트는 비어 있다가 복원 완료 시 채워진다.
    // (시트 접힘은 sheetState 애니메이션을 위해 PlayerBottomSheet 가 collapseSignal 로 직접 처리한다.)
    LaunchedEffect(expandSignal) {
        if (expandSignal > 0) {
            isShowPlayer = true
        }
    }

    // 공유받은 링크로 들어온 에피소드. 재생을 걸어 달라고만 하고 시트는 열지 않는다 —
    // 여는 것은 에피소드를 실제로 찾은 뒤 ViewModel 이 ShowPlayerBottomSheet 로 알린다.
    //
    // 여기서 미리 열면 링크가 잘못됐거나 오프라인일 때 되돌릴 사람이 없다. 올릴 것이 없어
    // state 가 Success 가 아니면 시트 본문이 통째로 그려지지 않는데(그 안에 스낵바 호스트도
    // 있다) isShowPlayer 만 참으로 남아, 한참 뒤 아무 에피소드나 재생하는 순간 부르지도
    // 않은 전체 화면 플레이어가 튀어나온다.
    //
    // 값이 아니라 읽는 함수로 받는 것은 EpisodiveNavHost 가 문서화한 캡처 함정과 같은 이유다.
    // 소비 콜백은 emit 뒤에 부른다 — 먼저 비우면 그 재구성이 이 이펙트를 취소해 액션이
    // 나가지 못한 채 요청만 사라진다.
    val pending = pendingEpisode()
    LaunchedEffect(pending) {
        val target = pending ?: return@LaunchedEffect
        viewModel.sendAction(
            PlayerAction.OpenDeepLink(
                episodeId = target.id,
                startPositionMs = target.startPositionMs,
            )
        )
        onPendingEpisodeHandled()
    }

    val unsavedMessage = stringResource(uiR.string.core_ui_snackbar_unsaved)
    val undoLabel = stringResource(uiR.string.core_ui_snackbar_undo)
    val sleepTimerExpiredMessage = stringResource(R.string.feature_player_sleep_timer_expired)
    val deepLinkErrorMessage = stringResource(R.string.feature_player_deep_link_not_found)
    val captionDownloadStartedPattern = stringResource(R.string.feature_player_caption_download_started)
    val captionUnsupportedMessage = stringResource(R.string.feature_player_caption_unsupported)
    val captionFailedNetworkMessage = stringResource(R.string.feature_player_caption_download_failed_network)
    val captionFailedStorageMessage = stringResource(R.string.feature_player_caption_download_failed_storage)
    val captionFailedCorruptMessage = stringResource(R.string.feature_player_caption_download_failed_corrupt)
    val captionFailedUnloadableMessage = stringResource(R.string.feature_player_caption_download_failed_unloadable)
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is PlayerEffect.NavigateToPodcast -> {}
                is PlayerEffect.ShowPlayerBottomSheet -> {
                    isShowPlayer = true
                }

                is PlayerEffect.HidePlayerBottomSheet -> {
                    isShowPlayer = false
                }

                is PlayerEffect.ShowUnsaveSnackbar -> {
                    if (!isShowPlayer) {
                        val undone = onShowSnackbar(unsavedMessage, undoLabel)
                        if (undone) viewModel.sendAction(PlayerAction.ToggleSavedEpisode(effect.episode))
                    }
                    // full player open → handled in PlayerBottomSheet
                }

                is PlayerEffect.ShowDeepLinkError -> {
                    // 실패했다는 것은 시트를 열지 않았다는 뜻이라 보통 여기가 띄운다. 앱 전역
                    // 스낵바 호스트라 재생 중인 것이 없어도 그려져 있다 — 시트 안 호스트는
                    // state 가 Success 일 때만 있어서, 없을 때 부르면 영영 뜨지 않는 데다
                    // showSnackbar 가 그 자리에서 멎어 이 수집 루프까지 함께 멈춘다.
                    if (!isShowPlayer) onShowSnackbar(deepLinkErrorMessage, null)
                }

                is PlayerEffect.SleepTimerExpired -> {
                    if (!isShowPlayer) {
                        onShowSnackbar(sleepTimerExpiredMessage, null)
                    }
                }

                is PlayerEffect.CaptionDownloadStarted -> {
                    if (!isShowPlayer) {
                        val message = String.format(
                            captionDownloadStartedPattern,
                            Formatter.formatShortFileSize(context, effect.sizeBytes),
                        )
                        onShowSnackbar(message, null)
                    }
                }

                is PlayerEffect.CaptionUnsupported -> {
                    if (!isShowPlayer) onShowSnackbar(captionUnsupportedMessage, null)
                }

                is PlayerEffect.CaptionDownloadFailed -> {
                    if (!isShowPlayer) {
                        val message = when (effect.reason) {
                            CaptionDownloadFailure.Reason.NETWORK -> captionFailedNetworkMessage
                            CaptionDownloadFailure.Reason.STORAGE -> captionFailedStorageMessage
                            CaptionDownloadFailure.Reason.CORRUPT -> captionFailedCorruptMessage
                            CaptionDownloadFailure.Reason.UNLOADABLE -> captionFailedUnloadableMessage
                        }
                        onShowSnackbar(message, null)
                    }
                }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AnimatedVisibility(
            visible = state is PlayerState.Success,
            modifier = Modifier.align(Alignment.BottomCenter), // 위치 유지
            enter = slideInVertically(
                initialOffsetY = { it }, // 자기 키만큼 아래에서 등장
                animationSpec = tween(300)
            ),
            exit = slideOutVertically(
                targetOffsetY = { it },
                animationSpec = tween(300)
            )
        ) {
            val s = state as? PlayerState.Success ?: return@AnimatedVisibility

            PlayerBarContent(
                modifier = modifier,
                podcast = s.podcast,
                nowPlaying = s.nowPlaying,
                progress = s.progress,
                isPlaying = s.isPlaying,
                chapters = s.chapters,
                onExpand = { viewModel.sendAction(PlayerAction.ExpandPlayer) },
                onToggleLike = { viewModel.sendAction(PlayerAction.ToggleLike) },
                onPlayOrPause = { viewModel.sendAction(PlayerAction.PlayOrPause) },
            )
        }
    }

    if (isShowPlayer) {
        PlayerBottomSheet(
            onPodcastClick = onPodcastClick,
            collapseSignal = collapseSignal,
        )
    }
}

@Composable
internal fun PlayerBarContent(
    modifier: Modifier = Modifier,
    podcast: Podcast,
    nowPlaying: Episode,
    progress: Progress,
    isPlaying: Boolean,
    chapters: List<Chapter>,
    onExpand: () -> Unit,
    onToggleLike: () -> Unit,
    onPlayOrPause: () -> Unit,
) {
    val dimension = LocalDimensionTheme.current
    // 미니플레이어는 콘텐츠 위에 떠 있는 어두운 틴트 칩이고 전경이 전부 흰색이다.
    // 블렌드 대상을 테마 표면색으로 두면 라이트 테마에서 배경이 근백색이 되어 글자가
    // 사라지므로, 테마와 무관한 다크 표면색으로 고정한다.
    val surfaceColor = PlayerBarTintSurface

    // 미니플레이어 배경은 지금 재생 중인 커버에서 뽑은 색으로 흐른다. 팔레트가 준비되기
    // 전에는 그 표면색을 써서 색이 튀지 않게 한다. 추출색을 그대로 깔지 않고 표면색 쪽으로
    // 눌러 쓰는 이유는, 밝은 커버에서 뽑힌 색 위에서는 흰 제목이 묻히기 때문이다.
    var dominantColor by remember { mutableStateOf(surfaceColor) }
    val barGradient = remember(dominantColor, surfaceColor) {
        Brush.linearGradient(
            colors = listOf(
                lerp(dominantColor, surfaceColor, PlayerBarTintStartBlend),
                lerp(dominantColor, surfaceColor, PlayerBarTintEndBlend),
            ),
            start = Offset.Zero,
            end = Offset(1000f, 176f),
        )
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(dimension.playerBarHeight)
            .padding(horizontal = dimension.playerBarMargin),
        shape = EpisodiveShapes.miniPlayer,
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent,
        ),
        elevation = CardDefaults.cardElevation(0.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.06f)),
        onClick = onExpand
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(barGradient)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                StateImage(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(11.dp)),
                    imageUrl = nowPlaying.coverUrl,
                    contentDescription = nowPlaying.title,
                    onDominantColorExtracted = { dominantColor = it },
                )

                Column(
                    modifier = Modifier
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    FadingEdgeText(
                        modifier = Modifier
                            .fillMaxWidth(),
                        text = nowPlaying.title,
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.White,
                        maxLines = 1,
                    )

                    FadingEdgeText(
                        modifier = Modifier
                            .fillMaxWidth(),
                        text = podcast.title,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.72f),
                        maxLines = 1,
                    )
                }

                EpisodiveIconToggleButton(
                    modifier = Modifier
                        .size(21.dp),
                    checked = nowPlaying.isLiked,
                    onCheckedChange = { onToggleLike() },
                    colors = IconButtonDefaults.iconToggleButtonColors(
                        checkedContainerColor = Color.Transparent,
                        checkedContentColor = Color.White.copy(alpha = 0.85f),
                        containerColor = Color.Transparent,
                        contentColor = Color.White.copy(alpha = 0.85f),
                    ),
                    icon = {
                        Icon(
                            modifier = Modifier.size(21.dp),
                            imageVector = EpisodiveIcons.Like,
                            contentDescription = "Like",
                            tint = Color.White.copy(alpha = 0.85f)
                        )
                    },
                    checkedIcon = {
                        Icon(
                            modifier = Modifier.size(21.dp),
                            imageVector = EpisodiveIcons.LikeFilled,
                            contentDescription = "Unlike",
                            tint = Color.White.copy(alpha = 0.85f)
                        )
                    }
                )

                EpisodiveIconToggleButton(
                    modifier = Modifier
                        .size(38.dp),
                    checked = isPlaying,
                    onCheckedChange = { onPlayOrPause() },
                    colors = IconButtonDefaults.iconToggleButtonColors(
                        checkedContainerColor = MaterialTheme.colorScheme.primary,
                        checkedContentColor = Color.White,
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = Color.White,
                    ),
                    icon = {
                        Icon(
                            modifier = Modifier.size(18.dp),
                            imageVector = EpisodiveIcons.Play,
                            contentDescription = "Play",
                            tint = Color.White
                        )
                    },
                    checkedIcon = {
                        Icon(
                            modifier = Modifier.size(18.dp),
                            imageVector = EpisodiveIcons.Pause,
                            contentDescription = "Pause",
                            tint = Color.White
                        )
                    }
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(dimension.progressThicknessThin)
                    .align(Alignment.BottomCenter)
                    .background(Color.White.copy(alpha = 0.2f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progress.positionRatio)
                        .fillMaxHeight()
                        .background(MaterialTheme.colorScheme.primary)
                )
            }
        }
    }
}

@DevicePreviews
@Composable
private fun PlayerBarPreview() {
    EpisodiveTheme {
        PlayerBarContent(
            podcast = podcastTestData,
            nowPlaying = episodeTestData,
            progress = Progress(
                position = 30.seconds,
                buffered = 60.seconds,
                duration = 100.seconds,
            ),
            isPlaying = false,
            chapters = listOf(
                Chapter("Chapter 1", 0.seconds, 10.seconds),
                Chapter("Chapter 2", 10.seconds, 80.seconds),
                Chapter("Chapter 3", 80.seconds, 100.seconds),
            ),
            onExpand = {},
            onToggleLike = {},
            onPlayOrPause = {},
        )
    }
}