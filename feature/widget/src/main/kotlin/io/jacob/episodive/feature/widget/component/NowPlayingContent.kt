package io.jacob.episodive.feature.widget.component

import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceModifier
import androidx.glance.LocalContext
import androidx.glance.action.actionParametersOf
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.CircularProgressIndicator
import androidx.glance.appwidget.cornerRadius
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import io.jacob.episodive.core.domain.widget.NowPlayingSnapshot
import io.jacob.episodive.core.domain.widget.PodcastSnapshot
import io.jacob.episodive.feature.widget.EpisodiveWidgetLayout
import io.jacob.episodive.feature.widget.FeedMode
import io.jacob.episodive.feature.widget.PlaybackControl
import io.jacob.episodive.feature.widget.R
import io.jacob.episodive.feature.widget.action.WIDGET_OPEN_PLAYER_PARAM
import io.jacob.episodive.feature.widget.action.mainActivityComponent

/**
 * 재생 + 나의 최신 피드 단일 위젯 콘텐츠(최상위 조합).
 *
 * - 배경: 썸네일 추출색 솔리드 (16dp 라운드, 그라데이션/스크림 없음)
 * - 1행(now playing): 썸네일 | (제목 1줄 · 팟캐스트명 · 컨트롤). 우상단 브랜드 로고
 * - 2행(피드): [EpisodiveWidgetLayout] 에 따라 STRIP/GRID/없음. 피드 컴포저블은 FeedContent.kt 참고.
 *   피드 영역만 추출색을 더 어둡게([feedBackgroundColor]).
 */
@Composable
internal fun NowPlayingContent(
    snapshot: NowPlayingSnapshot?,
    artwork: Bitmap?,
    backgroundColor: Int,
    feedBackgroundColor: Int,
    feed: List<PodcastSnapshot>,
    feedBitmaps: Map<Long, Bitmap?>,
    layout: EpisodiveWidgetLayout,
) {
    val showFeed = layout.feedMode != FeedMode.NONE && feed.isNotEmpty()
    val context = LocalContext.current
    val component = mainActivityComponent(context)
    Box(modifier = GlanceModifier.fillMaxSize()) {
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(ColorProvider(Color(backgroundColor)))
                .cornerRadius(16.dp)
                // 빈 영역 폴백: 앱만 연다.
                .clickable(actionStartActivity(component)),
        ) {
            // now-playing 이 남는 세로 공간을 차지(지배)하고, 피드는 모드별 고정 높이의 얇은 띠로 둔다.
            NowPlayingHeader(
                snapshot = snapshot,
                artwork = artwork,
                thumbDp = layout.nowPlayingThumbDp,
                // 2행(STRIP)·3행(GRID) 처럼 now-playing 영역이 높을 땐 제목 2줄, 1행(NONE)은 1줄.
                titleMaxLines = if (layout.feedMode == FeedMode.NONE) 1 else 2,
                modifier = (
                    if (showFeed) {
                        GlanceModifier.fillMaxWidth().defaultWeight()
                    } else {
                        GlanceModifier.fillMaxSize()
                    }
                    ).clickable(
                        // 재생 정보가 있으면 플레이어 화면(open_player), 없으면 앱만 연다. 스테이션이어도
                        // open_player 를 싣는다 — 앱의 딥링크 처리가 스테이션이 올라 있으면 라디오 화면으로 보낸다.
                        actionStartActivity(
                            component,
                            if (snapshot != null) {
                                actionParametersOf(WIDGET_OPEN_PLAYER_PARAM to true)
                            } else {
                                actionParametersOf()
                            },
                        ),
                    ),
            )
            if (showFeed) {
                FeedArea(
                    feed = feed,
                    feedBitmaps = feedBitmaps,
                    feedBackgroundColor = feedBackgroundColor,
                    layout = layout,
                    modifier = GlanceModifier
                        .fillMaxWidth()
                        .height(layout.feedHeightDp.dp),
                )
            }
        }
        BrandBadge()
    }
}

@Composable
private fun NowPlayingHeader(
    snapshot: NowPlayingSnapshot?,
    artwork: Bitmap?,
    thumbDp: Int,
    titleMaxLines: Int,
    modifier: GlanceModifier,
) {
    val context = LocalContext.current
    Row(
        modifier = modifier.padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val contentDescription = context.getString(R.string.feature_widget_now_playing_artwork_desc)
        val stationArt = snapshot?.stationArt
        if (stationArt != null && artwork == null) {
            // 프로그램 이미지가 없거나 받지 못했으면 이니셜 아트 그대로다.
            StationThumbnail(art = stationArt, sizeDp = thumbDp)
        } else {
            WidgetThumbnail(
                bitmap = artwork,
                contentDescription = contentDescription,
                sizeDp = thumbDp,
            )
        }
        Spacer(modifier = GlanceModifier.width(12.dp))
        // 높이를 가득 채워, 제목은 위·컨트롤은 아래로 고정(아래 weight Spacer)한다.
        Column(modifier = GlanceModifier.defaultWeight().fillMaxHeight()) {
            // 제목·부제만 우상단 브랜드 로고 공간을 피해 오른쪽 여백을 둔다.
            // 컨트롤은 아래에서 전체 폭(우측 정렬)을 써 브랜드 로고 위치(우측 끝)까지 닿게 한다.
            Column(modifier = GlanceModifier.fillMaxWidth().padding(end = 24.dp)) {
                Text(
                    text = snapshot?.title
                        ?: context.getString(R.string.feature_widget_now_playing_empty),
                    style = TextStyle(
                        color = ColorProvider(Color.White),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                    maxLines = titleMaxLines,
                )
                val subtitle = snapshot?.feedTitle?.takeIf { it.isNotBlank() }
                    ?: context.getString(R.string.feature_widget_now_playing_empty_hint)
                        .takeIf { snapshot == null }
                subtitle?.let {
                    Spacer(modifier = GlanceModifier.height(4.dp))
                    Text(
                        text = it,
                        style = TextStyle(
                            color = ColorProvider(WidgetOnArtworkSecondary),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                        ),
                        maxLines = 1,
                    )
                }
            }
            if (snapshot != null) {
                // 남는 세로 공간을 모두 밀어내 컨트롤을 now-playing 영역 하단에 붙인다.
                Spacer(modifier = GlanceModifier.defaultWeight())
                NowPlayingControlsRow(snapshot)
            }
        }
    }
}

@Composable
private fun NowPlayingControlsRow(snapshot: NowPlayingSnapshot) {
    val context = LocalContext.current
    // 컨트롤은 우측 정렬, 재생/정지를 가장 우측에 둔다. 앞의 Spacer(weight)가 나머지를 오른쪽으로 민다.
    Row(
        modifier = GlanceModifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Spacer(modifier = GlanceModifier.defaultWeight())
        // 라이브 스테이션은 탐색할 수 없다.
        if (!snapshot.isStation) {
            WidgetSeekButton(
                iconRes = R.drawable.feature_widget_ic_rewind,
                contentDescription = context.getString(
                    R.string.feature_widget_now_playing_seek_backward_desc,
                ),
                control = PlaybackControl.SEEK_BWD,
                sizeDp = 34,
                paddingDp = 6,
            )
            Spacer(modifier = GlanceModifier.width(4.dp))
            WidgetSeekButton(
                iconRes = R.drawable.feature_widget_ic_fast_forward,
                contentDescription = context.getString(
                    R.string.feature_widget_now_playing_seek_forward_desc,
                ),
                control = PlaybackControl.SEEK_FWD,
                sizeDp = 34,
                paddingDp = 6,
            )
            Spacer(modifier = GlanceModifier.width(10.dp))
        }
        WidgetPlayPauseButton(
            isPlaying = snapshot.isPlaying,
            contentDescription = context.getString(
                if (snapshot.isPlaying) {
                    R.string.feature_widget_now_playing_pause_desc
                } else {
                    R.string.feature_widget_now_playing_play_desc
                },
            ),
            diameter = 34,
        )
    }
}

/**
 * 사이즈 변경·재생/정지로 비트맵을 다시 로드하는 동안 위젯 전체를 덮는 로딩 화면.
 * placeholder 가 부분적으로 깜빡이는 대신 추출 배경색 위에 중앙 원형 프로그레스를 보여준다.
 */
@Composable
internal fun WidgetLoading(backgroundColor: Int) {
    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(ColorProvider(Color(backgroundColor)))
            .cornerRadius(16.dp),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(color = ColorProvider(Color.White))
    }
}

