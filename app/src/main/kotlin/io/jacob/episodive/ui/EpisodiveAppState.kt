package io.jacob.episodive.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import io.jacob.episodive.MainActivityViewModel
import io.jacob.episodive.core.data.util.NetworkMonitor
import io.jacob.episodive.core.model.share.EpisodiveDeepLink
import io.jacob.episodive.feature.home.navigation.HomeRoute
import io.jacob.episodive.feature.podcast.navigation.PodcastRoute
import io.jacob.episodive.feature.radio.navigation.RadioRoute
import io.jacob.episodive.feature.search.navigation.SearchRoute
import io.jacob.episodive.navigation.BottomBarDestination
import io.jacob.episodive.navigation.EpisodiveNavigationState
import io.jacob.episodive.navigation.EpisodiveNavigator
import io.jacob.episodive.navigation.rememberEpisodiveNavigationState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

@Composable
fun rememberEpisodiveAppState(
    networkMonitor: NetworkMonitor,
    viewModel: MainActivityViewModel,
    coroutineScope: CoroutineScope = rememberCoroutineScope(),
): EpisodiveAppState {
    val navigationState = rememberEpisodiveNavigationState(
        startRoute = HomeRoute,
        topLevelRoutes = BottomBarDestination.entries.map { it.navKey }.toSet(),
    )
    val navigator = remember(navigationState) { EpisodiveNavigator(navigationState) }

    return remember(networkMonitor, navigationState, navigator, coroutineScope) {
        EpisodiveAppState(
            networkMonitor = networkMonitor,
            viewModel = viewModel,
            navigationState = navigationState,
            navigator = navigator,
            coroutineScope = coroutineScope,
        )
    }
}

class EpisodiveAppState(
    networkMonitor: NetworkMonitor,
    val viewModel: MainActivityViewModel,
    val navigationState: EpisodiveNavigationState,
    val navigator: EpisodiveNavigator,
    coroutineScope: CoroutineScope,
) {
    val bottomBarDestinations: List<BottomBarDestination> = BottomBarDestination.entries

    /**
     * 검색 화면이 열리자마자 입력창에 포커스를 줘야 하는지. 홈의 검색 바로가기로 들어올
     * 때만 켜지고, 검색 화면이 처리한 뒤 [consumeSearchAutoFocus] 로 곧바로 꺼진다.
     *
     * 상태로 두는 이유는 홈에서 검색 탭까지가 한 프레임에 끝나지 않기 때문이다. 일회성
     * 이벤트로 쏘면 아직 구독하지 않은 검색 화면을 지나쳐 사라진다. 검색이 로딩 중이면
     * 이 값이 그대로 남아 있다가 입력창이 실제로 그려질 때 쓰인다.
     *
     * 저장 상태로 만들지 않은 것은 의도한 선택이다. 화면 회전으로 이 값이 날아가면 포커스가
     * 한 번 불발되는 데 그치지만, 살려 두면 회전 뒤에 사용자가 부르지 않은 키보드가 뒤늦게
     * 올라온다. 불발이 오작동보다 낫다.
     */
    var searchAutoFocus by mutableStateOf(false)
        private set

    /**
     * 공유받은 링크로 열어야 할 에피소드. 플레이어가 집어 가면 [consumeEpisodeDeepLink] 로 비운다.
     *
     * [searchAutoFocus] 와 같은 이유로 상태다. 딥링크는 앱이 뜨는 것과 같은 프레임에 도착하는데
     * `PlayerBar` 는 그보다 늦게 구독을 시작하므로, 일회성 이벤트로 쏘면 콜드 스타트에서
     * 그대로 지나쳐 사라진다.
     */
    var pendingEpisodeDeepLink by mutableStateOf<EpisodiveDeepLink.Episode?>(null)
        private set

    fun requestEpisodeDeepLink(target: EpisodiveDeepLink.Episode) {
        pendingEpisodeDeepLink = target
    }

    fun consumeEpisodeDeepLink() {
        pendingEpisodeDeepLink = null
    }

    fun navigateToBottomBarDestination(destination: BottomBarDestination) {
        val route = destination.navKey
        if (route == navigationState.topLevelRoute) {
            navigator.navigateToTabRoot()
        } else {
            navigator.navigate(route)
        }
    }

    fun navigateToPodcast(podcastId: Long) {
        navigator.navigate(PodcastRoute(podcastId))
    }

    fun navigateToRadio() {
        navigator.navigateInStartStack(RadioRoute)
    }

    /** 홈의 검색 바로가기. 검색 탭으로 옮기면서 "바로 입력할 참이다"는 신호를 같이 켠다. */
    fun navigateToSearchWithFocus() {
        navigator.navigate(SearchRoute)
        // 탭 전환만으로는 그 탭에 쌓여 있던 화면이 그대로 되살아난다. 검색하다 팟캐스트
        // 상세로 파고든 채 떠났다면 검색창 대신 그 상세가 뜨므로, 루트까지 되감는다.
        navigator.navigateToTabRoot()
        searchAutoFocus = true
    }

    fun consumeSearchAutoFocus() {
        searchAutoFocus = false
    }

    /**
     * 검색 탭을 벗어났다면 대기 중이던 포커스 신호를 버린다.
     *
     * 신호는 검색바가 실제로 그려질 때만 소비되는데, 검색이 로딩·에러라 검색바가 아직 없는
     * 사이에 다른 탭으로 가버리면 켜진 채로 남는다. 그대로 두면 한참 뒤 하단 바로 검색에
     * 들어올 때 부르지 않은 키보드가 올라온다.
     */
    fun discardSearchAutoFocusIfTabLeft() {
        if (navigationState.topLevelRoute != SearchRoute) consumeSearchAutoFocus()
    }

    val isOffline = networkMonitor.isOnline
        .map(Boolean::not)
        .stateIn(
            scope = coroutineScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = false,
        )
}
