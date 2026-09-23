package io.jacob.episodive.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import io.jacob.episodive.feature.channel.navigation.ChannelRoute
import io.jacob.episodive.feature.channel.navigation.channelEntries
import io.jacob.episodive.feature.clip.navigation.clipEntries
import io.jacob.episodive.feature.home.navigation.HomeMoreRoute
import io.jacob.episodive.feature.home.navigation.homeEntries
import io.jacob.episodive.feature.library.navigation.libraryEntries
import io.jacob.episodive.feature.podcast.navigation.PodcastRoute
import io.jacob.episodive.feature.podcast.navigation.podcastEntries
import io.jacob.episodive.feature.search.navigation.searchEntries

@Composable
fun EpisodiveNavHost(
    navigationState: EpisodiveNavigationState,
    navigator: EpisodiveNavigator,
    onShowSnackbar: suspend (message: String, actionLabel: String?) -> Boolean,
    modifier: Modifier = Modifier,
    onSearchShortcutClick: () -> Unit = {},
    // 값이 아니라 읽는 함수를 받는다. navigation3 가 NavEntry 를 재사용하는 탓에 아래
    // entryProvider 블록은 처음 캡처한 값에 갇힌다 — 자세한 이유는 searchEntries 문서 참고.
    searchAutoFocus: () -> Boolean = { false },
    onSearchAutoFocusHandled: () -> Unit = {},
) {
    val entryProvider = entryProvider<NavKey> {
        homeEntries(
            onPodcastClick = { navigator.navigate(PodcastRoute(it)) },
            onChannelClick = { navigator.navigate(ChannelRoute(it)) },
            onMoreClick = { navigator.navigate(HomeMoreRoute(it)) },
            onBackClick = { navigator.goBack() },
            // 탭 전환에 포커스 신호까지 얹어야 해서 navigator 를 직접 부르지 않는다.
            onSearchClick = onSearchShortcutClick,
            onShowSnackbar = onShowSnackbar,
        )
        searchEntries(
            onPodcastClick = { navigator.navigate(PodcastRoute(it)) },
            onShowSnackbar = onShowSnackbar,
            autoFocus = searchAutoFocus,
            onAutoFocusHandled = onSearchAutoFocusHandled,
        )
        libraryEntries(
            onPodcastClick = { navigator.navigate(PodcastRoute(it)) },
            onShowSnackbar = onShowSnackbar,
        )
        clipEntries(
            onPodcastClick = { navigator.navigate(PodcastRoute(it)) },
            onShowSnackbar = onShowSnackbar,
        )
        podcastEntries(
            onBackClick = { navigator.goBack() },
            onShowSnackbar = onShowSnackbar,
        )
        channelEntries(
            onBackClick = { navigator.goBack() },
            onPodcastClick = { navigator.navigate(PodcastRoute(it)) },
            onShowSnackbar = onShowSnackbar,
        )
    }

    // 람다만 기억해 둔다. 탭/뒤로 판정은 스펙이 불릴 때마다 플래그를 읽어서 하므로 결과를 캐시하면 안 된다.
    val popTransitionSpec = remember(navigationState) {
        tabAwarePopTransitionSpec<NavKey> { navigationState.isTabNavigation }
    }
    val predictivePopTransitionSpec = remember { predictiveBackTransitionSpec<NavKey>() }

    NavDisplay(
        entries = navigationState.toDecoratedEntries(entryProvider),
        onBack = { navigator.goBack() },
        popTransitionSpec = popTransitionSpec,
        predictivePopTransitionSpec = predictivePopTransitionSpec,
        modifier = modifier,
    )
}
