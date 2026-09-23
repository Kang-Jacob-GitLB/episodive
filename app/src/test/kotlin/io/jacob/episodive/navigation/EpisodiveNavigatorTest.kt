package io.jacob.episodive.navigation

import androidx.compose.runtime.mutableStateOf
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import io.jacob.episodive.feature.home.navigation.HomeRoute
import io.jacob.episodive.feature.podcast.navigation.PodcastRoute
import io.jacob.episodive.feature.search.navigation.SearchRoute
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * pop 전환이 뒤로가기인지 탭 전환인지 가르는 [EpisodiveNavigationState.isTabNavigation] 의 계약.
 *
 * navigation3 는 스택이 줄어드는 모양만 보고 pop 을 판정해서, 다른 탭에서 홈 탭을 누르는 것과
 * 뒤로가기를 구분하지 못한다. 그 구분은 여기서만 드러난다 — 틀리면 탭을 누를 때 뒤로가기처럼
 * 화면이 줄어들거나, 뒤로가기가 탭 전환 페이드로 바뀐다.
 */
class EpisodiveNavigatorTest {

    private val state = EpisodiveNavigationState(
        startRoute = HomeRoute,
        topLevelRoute = mutableStateOf<NavKey>(HomeRoute),
        backStacks = listOf<NavKey>(HomeRoute, SearchRoute).associateWith { NavBackStack(it) },
    )
    private val navigator = EpisodiveNavigator(state)

    @Test
    fun selectingTab_isTabNavigation() {
        navigator.navigate(SearchRoute)

        assertTrue(state.isTabNavigation)
    }

    @Test
    fun returningToStartTab_isTabNavigation() {
        // 스택이 [홈, 검색] → [홈] 으로 줄어 navigation3 에는 pop 으로 보이는 경우다.
        navigator.navigate(SearchRoute)
        navigator.navigate(PodcastRoute(42L))

        navigator.navigate(HomeRoute)

        assertTrue(state.isTabNavigation)
    }

    @Test
    fun reselectingCurrentTab_isTabNavigation() {
        navigator.navigate(SearchRoute)
        navigator.navigate(PodcastRoute(42L))

        navigator.navigateToTabRoot()

        assertTrue(state.isTabNavigation)
    }

    @Test
    fun openingScreenAfterTabSelection_clearsTabNavigation() {
        navigator.navigate(SearchRoute)

        navigator.navigate(PodcastRoute(42L))

        assertFalse(state.isTabNavigation)
    }

    @Test
    fun goingBackAfterTabSelection_isNotTabNavigation() {
        navigator.navigate(SearchRoute)
        navigator.navigate(PodcastRoute(42L))
        navigator.navigateToTabRoot()
        navigator.navigate(PodcastRoute(42L))

        navigator.goBack()

        assertFalse(state.isTabNavigation)
    }

    @Test
    fun goingBackFromTabRoot_isBackNotTabNavigation() {
        // 탭 루트에서의 뒤로가기는 홈 탭으로 바뀌어도 뒤로가기다. 제스처로 하면 미리보기가
        // 돌기 때문에 버튼도 같은 전환을 써야 한다.
        navigator.navigate(SearchRoute)

        navigator.goBack()

        assertEquals(HomeRoute, state.topLevelRoute)
        assertFalse(state.isTabNavigation)
    }
}
