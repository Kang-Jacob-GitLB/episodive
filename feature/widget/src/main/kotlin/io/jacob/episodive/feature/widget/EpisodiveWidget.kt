package io.jacob.episodive.feature.widget

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.glance.GlanceId
import androidx.glance.LocalSize
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.provideContent
import dagger.hilt.android.EntryPointAccessors
import io.jacob.episodive.core.domain.widget.WidgetDataReaderEntryPoint
import io.jacob.episodive.feature.widget.component.NowPlayingContent
import io.jacob.episodive.feature.widget.component.WidgetLoading
import io.jacob.episodive.feature.widget.image.WidgetColorExtractor
import io.jacob.episodive.feature.widget.image.WidgetImageLoader
import io.jacob.episodive.feature.widget.theme.EpisodiveGlanceTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext

/**
 * 재생 + 나의 최신 피드 단일 위젯 (리사이즈 3x1~4x3, 폭 3~4열·세로 1~3행).
 *
 * 핵심 규약:
 * - 데이터는 반드시 `provideContent` 컴포지션 *안*에서 `collectAsState` 로 구독한다.
 *   provideGlance 본문은 세션당 1회만 실행되므로 거기서 읽으면 updateAll/리사이즈 시 stale.
 *   재생 정보(`nowPlayingFlow`)와 피드(`userRecentPodcastsFlow`) 모두 동일 규약.
 * - `SizeMode.Exact`: 현재 크기만 컴포즈 → [LocalSize] 높이로 3단계 분기.
 *   (Responsive 는 선언한 모든 사이즈 컴포지션을 한 RemoteViews 에 담아 비트맵
 *    페이로드가 배가되어 ~1MB Binder 한도를 쉽게 초과하므로 쓰지 않는다.)
 * - 보이는 피드 항목만 비트맵 로드 + 작은 px → RemoteViews 1MB 한도 보호.
 */
class EpisodiveWidget : GlanceAppWidget() {

    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val reader = EntryPointAccessors
            .fromApplication(
                context.applicationContext,
                WidgetDataReaderEntryPoint::class.java,
            )
            .widgetDataReader()

        provideContent {
            val snapshot by remember { reader.nowPlayingFlow() }.collectAsState(null)
            val podcasts by remember { reader.userRecentPodcastsFlow(FEED_MAX) }
                .collectAsState(emptyList())
            val size = LocalSize.current
            val layout = EpisodiveWidgetLayout.forSize(size)
            // 표시 크기에 맞춘 비트맵 로드 px (밀도 반영). 작게 고정하면 업스케일로 흐려진다.
            // now-playing 은 1장이라 페이로드 비중이 커서 별도 상한([NOW_PLAYING_MAX_PX])을 둔다.
            val density = context.resources.displayMetrics.density
            val nowPlayingPx = (layout.nowPlayingThumbDp * density).toInt()
                .coerceAtMost(NOW_PLAYING_MAX_PX)
            val feedPx = layout.feedThumbPx(density)

            // 비트맵 로드와 배경색 추출(Palette)을 한 producer 에서 한 패스로 처리해 결과
            // (비트맵 + 배경/피드 색)를 함께 게시한다. Palette 는 컴포지션의 remember 가 아닌
            // 로드 직후 백그라운드(Default)에서 끝낸다.
            // 결과에 "어떤 key(url + px)로 로드했는지"를 함께 담아, 현재 key 와 일치할 때만 완료로
            // 보고 불일치(초기 null·키 변경 직후)는 곧바로 로딩으로 판정한다 → produceState 의 value
            // 가 비동기로 갱신되며 생기는 "키 변경 직후 한 프레임은 직전 값" 레이스를 동기적으로 회피.
            // (로드 실패로 비트맵이 null 이어도 key 는 일치하므로 무한 스피너가 되지 않는다.)
            val artUrl = snapshot?.imageUrl?.takeIf { it.isNotBlank() }
            val artKey = artUrl to nowPlayingPx
            val isStation = snapshot?.isStation == true
            val artworkLoaded by produceState<ArtworkResult?>(null, artUrl, nowPlayingPx, isStation) {
                // 스테이션은 비트맵도 Palette 추출도 없다. 배경은 아래에서 이니셜 아트 색으로 정한다.
                if (isStation) {
                    value = null
                    return@produceState
                }
                val bitmap = artUrl?.let {
                    WidgetImageLoader.loadWidgetBitmap(context, it, nowPlayingPx)
                }
                val colors = withContext(Dispatchers.Default) {
                    WidgetColorExtractor.colors(bitmap)
                }
                value = ArtworkResult(artKey, bitmap, colors)
            }
            val readyArtwork = artworkLoaded?.takeIf { it.key == artKey }
            val artwork = readyArtwork?.bitmap
            // 스테이션은 이니셜 아트 색을 어둡게 한 톤이 배경이다. 썸네일이 아트 색 그대로라 경계가 보인다.
            val stationColor = snapshot?.stationArt?.colorArgb
            val background = if (stationColor != null) {
                WidgetColorExtractor.fromColor(WidgetColorExtractor.feedBackgroundColor(stationColor))
            } else {
                readyArtwork?.colors ?: WidgetColorExtractor.colors(null)
            }
            val backgroundColor = background.background
            val feedBackgroundColor = background.feed

            // 현재 크기에서 보이는 만큼만 로드 (최대 4x3 GRID = 8개) → 비트맵 페이로드 상한.
            val visibleFeed = remember(podcasts, layout.feedCount) {
                podcasts.take(layout.feedCount)
            }
            val feedKey = visibleFeed.map { it.id } to feedPx
            val feedLoaded by produceState<Pair<Pair<List<Long>, Int>, Map<Long, Bitmap?>>?>(
                null, visibleFeed, feedPx,
            ) {
                value = feedKey to coroutineScope {
                    visibleFeed
                        .map { snap ->
                            async {
                                snap.id to WidgetImageLoader.loadWidgetBitmap(
                                    context,
                                    snap.imageUrl,
                                    feedPx,
                                )
                            }
                        }
                        .awaitAll()
                        .toMap()
                }
            }
            val feedReady = feedLoaded?.first == feedKey
            val feedBitmaps = if (feedReady) feedLoaded?.second ?: emptyMap() else emptyMap()

            // 사이즈 변경/재생·정지로 컴포지션이 새로 시작되면 비트맵을 다시 로드한다.
            // 그동안 placeholder 가 부분적으로 깜빡이지 않도록, 로드 대상이 있는데 아직 현재 key 의
            // 결과가 준비되지 않았으면 위젯 전체를 원형 프로그레스로 덮는다(빈 상태는 제외).
            val isLoading =
                (artUrl != null && readyArtwork == null) ||
                    (visibleFeed.isNotEmpty() && !feedReady)

            EpisodiveGlanceTheme {
                if (isLoading) {
                    WidgetLoading(backgroundColor = backgroundColor)
                } else {
                    NowPlayingContent(
                        snapshot = snapshot,
                        artwork = artwork,
                        backgroundColor = backgroundColor,
                        feedBackgroundColor = feedBackgroundColor,
                        feed = visibleFeed,
                        feedBitmaps = feedBitmaps,
                        layout = layout,
                    )
                }
            }
        }
    }

    private companion object {
        /** 가장 큰 4x3 GRID 에서 필요한 그리드 항목 수(2×4=8). */
        const val FEED_MAX = 8

        /** now-playing 비트맵 로드 px 상한. 표시 dp([EpisodiveWidgetLayout.nowPlayingThumbDp])×밀도를 이 값으로 캡. */
        const val NOW_PLAYING_MAX_PX = 224
    }
}

/** 한 번의 로드 패스 결과: 로드한 key(url+px) + 비트맵 + 그로부터 추출한 배경/피드 색. */
private data class ArtworkResult(
    val key: Pair<String?, Int>,
    val bitmap: Bitmap?,
    val colors: WidgetColorExtractor.WidgetBackground,
)
