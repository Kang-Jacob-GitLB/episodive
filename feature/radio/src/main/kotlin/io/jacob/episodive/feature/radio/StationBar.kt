package io.jacob.episodive.feature.radio

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import io.jacob.episodive.core.designsystem.component.EpisodiveIconButton
import io.jacob.episodive.core.designsystem.component.FadingEdgeText
import io.jacob.episodive.core.designsystem.component.MiniBarContainer
import io.jacob.episodive.core.designsystem.component.MiniBarPlayPauseButton
import io.jacob.episodive.core.designsystem.icon.EpisodiveIcons
import io.jacob.episodive.core.designsystem.theme.EpisodiveTheme
import io.jacob.episodive.core.designsystem.tooling.DevicePreviews
import io.jacob.episodive.core.testing.model.radioStationTestData
import io.jacob.episodive.core.ui.LiveBadge
import io.jacob.episodive.core.ui.StationCover
import io.jacob.episodive.core.ui.rememberStationCoverColor
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import io.jacob.episodive.core.ui.R as uiR

/**
 * 라디오 미니바. 진행바·시크가 없다 — 라이브라 갈 곳도 남은 시간도 없다.
 *
 * 이 바와 에피소드 미니바는 한 자리를 다툰다. 어느 쪽을 보일지는 [state] 를 쥔 쪽(EpisodiveApp)이
 * 정해 같은 프레임에 둘을 바꾼다 — 각자 따로 판정하면 한 프레임 겹쳐 보인다.
 */
@Composable
fun StationBar(
    state: StationBarState,
    modifier: Modifier = Modifier,
    viewModel: StationBarViewModel = hiltViewModel(),
    onClick: () -> Unit,
    onShowSnackbar: suspend (message: String, actionLabel: String?) -> Boolean,
) {
    val unplayableFormat = stringResource(R.string.feature_radio_unplayable_format)
    val unavailableFormat = stringResource(uiR.string.core_ui_station_unavailable_format)
    val sleepTimerExpiredMessage = stringResource(uiR.string.core_ui_sleep_timer_expired)
    // 수집은 한 번만 시작한다. 라우팅(시트 호스트/전역 호스트)이 바뀌어도 앞 콜백에 갇히지 않게 읽는 함수로 쥔다.
    val currentOnShowSnackbar by rememberUpdatedState(onShowSnackbar)

    // 수집은 AnimatedVisibility 밖에 둔다. 감독자는 항목을 비운 뒤에 재생 불가를 알리므로, 이펙트는 바가
    // 사라지는 순간에 도착한다.
    LaunchedEffect(Unit) {
        // onShowSnackbar 는 스낵바가 닫힐 때까지 돌아오지 않는다. 인라인으로 기다리면 그동안 다음 이펙트를
        // 못 받으므로, 따로 띄우고 새 이펙트가 오면 앞의 것을 취소해 최신 하나만 남긴다.
        var snackbar: Job? = null
        viewModel.effect.collect { effect ->
            val message = when (effect) {
                is StationBarEffect.ShowUnplayable -> String.format(unplayableFormat, effect.station.name)
                is StationBarEffect.ShowUnavailable -> String.format(unavailableFormat, effect.station.name)
                is StationBarEffect.ShowSleepTimerExpired -> sleepTimerExpiredMessage
            }
            snackbar?.cancel()
            snackbar = launch { currentOnShowSnackbar(message, null) }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AnimatedVisibility(
            visible = state is StationBarState.Active,
            modifier = Modifier.align(Alignment.BottomCenter),
            enter = slideInVertically(
                initialOffsetY = { it },
                animationSpec = tween(300)
            ),
            exit = slideOutVertically(
                targetOffsetY = { it },
                animationSpec = tween(300)
            )
        ) {
            val s = state as? StationBarState.Active ?: return@AnimatedVisibility

            StationBarContent(
                modifier = modifier,
                state = s,
                onClick = onClick,
                onPlayOrPause = { viewModel.sendAction(StationBarAction.PlayOrPause) },
                onStop = { viewModel.sendAction(StationBarAction.Stop) },
            )
        }
    }
}

@Composable
internal fun StationBarContent(
    modifier: Modifier = Modifier,
    state: StationBarState.Active,
    onClick: () -> Unit,
    onPlayOrPause: () -> Unit,
    onStop: () -> Unit,
) {
    val station = state.station
    val accent = rememberStationCoverColor(station, state.program?.imageUrl)

    MiniBarContainer(
        modifier = modifier,
        accent = accent,
        onClick = onClick,
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            StationCover(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(11.dp)),
                station = station,
                imageUrl = state.program?.imageUrl,
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                FadingEdgeText(
                    modifier = Modifier.fillMaxWidth(),
                    text = station.name,
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White,
                    maxLines = 1,
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    LiveBadge()

                    // 곡(MBC) → ICY 제목 → 프로그램 제목 순으로 보여 주고, 다 없으면 방송사를 보여 준다.
                    FadingEdgeText(
                        modifier = Modifier.weight(1f),
                        text = state.program?.song?.takeIf { it.isNotBlank() }
                            ?: state.streamTitle?.takeIf { it.isNotBlank() }
                            ?: state.program?.title?.takeIf { it.isNotBlank() }
                            // 방송사를 모르는 해외 스테이션은 지역으로 대신한다(둘 다 없으면 빈 문자열).
                            ?: station.broadcaster.ifBlank { station.region.orEmpty() },
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.72f),
                        maxLines = 1,
                    )
                }
            }

            EpisodiveIconButton(
                modifier = Modifier.size(21.dp),
                onClick = onStop,
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = Color.Transparent,
                    contentColor = Color.White.copy(alpha = 0.85f),
                ),
                icon = {
                    Icon(
                        modifier = Modifier.size(21.dp),
                        imageVector = EpisodiveIcons.Stop,
                        contentDescription = stringResource(R.string.feature_radio_stop),
                        tint = Color.White.copy(alpha = 0.85f),
                    )
                },
            )

            MiniBarPlayPauseButton(
                isPlaying = state.isPlaying,
                isBuffering = state.isBuffering,
                onClick = onPlayOrPause,
            )
        }
    }
}

@DevicePreviews
@Composable
private fun StationBarPreview() {
    EpisodiveTheme {
        StationBarContent(
            state = StationBarState.Active(
                station = radioStationTestData,
                streamTitle = "Artist - Title",
                isPlaying = true,
                isBuffering = false,
            ),
            onClick = {},
            onPlayOrPause = {},
            onStop = {},
        )
    }
}
