package io.jacob.episodive.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.NavEntryDecorator
import androidx.navigation3.scene.Scene
import androidx.navigation3.ui.defaultPopTransitionSpec

// 전환 총길이. 제스처 도중에는 진행률로 시크하므로 이 값이 드러나지 않지만, 손을 놓은 뒤
// navigation3 가 남은 구간을 `(1 - 진행률) × 총길이` 로 선형 재생하므로 확정·취소 속도가 된다
// (navigation3 1.0.1 바이트코드로 확인한 동작이다. 올릴 때 다시 볼 것). 버튼 뒤로가기에서는
// 이 길이 그대로 재생된다.
private const val BACK_DURATION_MILLIS = 400

// 이 진행률에서 나가는 화면이 완전히 사라지고 들어오는 화면이 나타나기 시작한다.
private const val BACK_FADE_THROUGH = 0.35f

// SystemUI 의 뒤로가기 미리보기와 같은 곡선. 제스처 초반에 반응이 크게 보이게 한다.
private val BackEasing = CubicBezierEasing(0.1f, 0.1f, 0f, 1f)

/**
 * 뒤로가기 전환. Android 디자인 가이드의 전체 화면 predictive back 수치를 따른다 —
 * 나가는 화면은 90% 로 줄며 35% 까지 사라지고, 들어오는 화면은 110% 에서 제자리로
 * 오며 35% 부터 나타난다.
 *
 * 제스처(미리보기)와 버튼(시스템 뒤로 버튼·화면의 뒤로 화살표)이 같은 모양을 쓴다.
 * navigation3 기본값은 제스처가 `scaleOut(0.7)` + `fadeIn`, 버튼이 700ms 페이드라 나가는
 * 방법에 따라 달랐다. 가장자리 쪽으로 미는 이동은 공유 요소 전환용 수치라 넣지 않는다. 그래서
 * 어느 가장자리에서 스와이프하든 같은 모양이다.
 */
private fun backContentTransform(): ContentTransform {
    val fadeOutMillis = (BACK_DURATION_MILLIS * BACK_FADE_THROUGH).toInt()
    val scaleSpec = tween<Float>(BACK_DURATION_MILLIS, easing = BackEasing)

    val enter = fadeIn(
        tween(
            durationMillis = BACK_DURATION_MILLIS - fadeOutMillis,
            delayMillis = fadeOutMillis,
            easing = LinearEasing,
        ),
    ) + scaleIn(scaleSpec, initialScale = 1.1f)
    val exit = fadeOut(tween(fadeOutMillis, easing = LinearEasing)) +
        scaleOut(scaleSpec, targetScale = 0.9f)

    return enter togetherWith exit
}

/** 제스처 뒤로가기 미리보기. 스와이프 가장자리와 무관하게 [backContentTransform] 을 쓴다. */
fun <T : Any> predictiveBackTransitionSpec(): AnimatedContentTransitionScope<Scene<T>>.(Int) -> ContentTransform =
    { backContentTransform() }

/**
 * 제스처가 아닌 pop.
 *
 * 탭 조작으로 스택이 줄어드는 것도 navigation3 에는 pop 으로 보인다 — 다른 탭에서 홈 탭을
 * 누르면 entries 가 `[홈…, 그 탭…]` 에서 `[홈…]` 로 줄어 뒤로가기와 모양이 같다. 그건 탭
 * 전환이지 뒤로가기가 아니므로 [isTabNavigation] 이 참이면 다른 탭 전환과 같은 기본 페이드를 둔다.
 */
fun <T : Any> tabAwarePopTransitionSpec(
    isTabNavigation: () -> Boolean,
): AnimatedContentTransitionScope<Scene<T>>.() -> ContentTransform {
    val tabTransitionSpec = defaultPopTransitionSpec<T>()
    return { if (isTabNavigation()) tabTransitionSpec() else backContentTransform() }
}

/**
 * 화면마다 불투명 배경을 깐다.
 *
 * 전환 애니메이션은 entry 단위로 크기와 투명도를 바꾸는데, 배경을 칠하지 않는 화면(예:
 * `FadeTopBarLayout` 을 쓰는 팟캐스트·채널)은 그동안 뒤 화면이 비쳐 보인다. 그 화면들이
 * 기대던 앱 Scaffold 의 컨테이너는 전환 바깥에 있어 함께 줄어들지 않으므로 여기서 칠한다.
 * 색은 그 컨테이너(`containerColor` 기본값)와 `EpisodiveScaffold` 가 쓰는 `background` 로
 * 맞춘다 — 전환 중 드러나는 뒷배경과 같아야 경계가 보이지 않는다. 이미 배경이 있는 화면은
 * 그 위에 그려지니 보이는 것이 달라지지 않는다.
 */
@Composable
fun <T : Any> rememberOpaqueBackgroundNavEntryDecorator(): NavEntryDecorator<T> = remember {
    NavEntryDecorator { entry ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
        ) {
            entry.Content()
        }
    }
}
