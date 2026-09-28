package io.jacob.episodive.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.exclude
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.jacob.episodive.DeepLinkEvent
import io.jacob.episodive.R
import io.jacob.episodive.core.designsystem.component.EpisodiveBackground
import io.jacob.episodive.core.designsystem.component.EpisodiveSwipeDismissSnackbarHost
import io.jacob.episodive.core.designsystem.theme.LocalDimensionTheme
import io.jacob.episodive.core.designsystem.component.EpisodiveNavigationBar
import io.jacob.episodive.core.designsystem.component.EpisodiveNavigationBarItem
import io.jacob.episodive.core.designsystem.component.MorphIcon
import io.jacob.episodive.feature.onboarding.OnboardingRoute
import io.jacob.episodive.core.ui.LocalIsPlaying
import io.jacob.episodive.core.ui.LocalNowPlayingEpisodeId
import io.jacob.episodive.feature.player.PlayerBar
import io.jacob.episodive.navigation.EpisodiveNavHost

@Composable
fun EpisodiveApp(
    appState: EpisodiveAppState,
    modifier: Modifier = Modifier,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val isOffline by appState.isOffline.collectAsStateWithLifecycle()

    val notConnectedMessage = stringResource(R.string.not_connected)
    LaunchedEffect(isOffline) {
        if (isOffline) {
            snackbarHostState.showSnackbar(
                message = notConnectedMessage,
                duration = SnackbarDuration.Long
            )
        }
    }

    EpisodiveBackground(modifier = modifier) {
        EpisodiveApp(
            appState = appState,
            snackbarHostState = snackbarHostState,
        )
    }
}

@Composable
fun EpisodiveApp(
    appState: EpisodiveAppState,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState,
) {
    val state by appState.viewModel.state.collectAsStateWithLifecycle()

    val onShowSnackbar: suspend (String, String?) -> Boolean = remember {
        { message: String, action: String? ->
            snackbarHostState.showSnackbar(
                message = message,
                actionLabel = action,
                duration = if (action != null) SnackbarDuration.Long else SnackbarDuration.Short,
            ) == SnackbarResult.ActionPerformed
        }
    }

    if (state.isFirstLaunch()) {
        Box(
            modifier = Modifier
                .fillMaxSize(),
        ) {
            OnboardingRoute(
                onShowSnackbar = onShowSnackbar,
            )

            EpisodiveSwipeDismissSnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .windowInsetsPadding(WindowInsets.safeDrawing)
                    .padding(bottom = 70.dp)
            )
        }
        return
    }

    LaunchedEffect(appState.navigationState.topLevelRoute) {
        appState.discardSearchAutoFocusIfTabLeft()
    }

    // 위젯 딥링크 → 플레이어 시트 펼침/접힘 신호. 콜드(fresh onCreate) 타이밍에 일회성 effect 가
    // PlayerBar 구독 전 emit 되어 유실되지 않도록, 상태(시그널) 로 PlayerBar 에 전달한다.
    var expandPlayerSignal by remember { mutableIntStateOf(0) }
    var collapsePlayerSignal by remember { mutableIntStateOf(0) }

    // Deep link handling
    LaunchedEffect(Unit) {
        appState.viewModel.deepLinkEvent.collect { event ->
            when (event) {
                is DeepLinkEvent.Podcast -> {
                    appState.navigateToPodcast(event.id)
                    // 앱 내 플레이어에서 팟캐스트 탭 시와 동일하게, 열린 시트가 가리지 않도록 접는다.
                    collapsePlayerSignal++
                    appState.viewModel.consumeDeepLink()
                }

                is DeepLinkEvent.Player -> {
                    expandPlayerSignal++
                    appState.viewModel.consumeDeepLink()
                }

                is DeepLinkEvent.Episode -> {
                    // 시트를 여는 것과 재생을 거는 것 모두 PlayerBar 가 한다 — PlayerViewModel 은
                    // 그 안에서 만들어져 여기서는 손이 닿지 않는다.
                    appState.requestEpisodeDeepLink(event.target)
                    appState.viewModel.consumeDeepLink()
                }
            }
        }
    }

    // POST_NOTIFICATIONS runtime permission request (Android 13+)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val permissionLauncher = rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { /* granted or denied — no action needed */ }

        val context = LocalContext.current
        LaunchedEffect(Unit) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    Scaffold(
        modifier = modifier,
        bottomBar = {
            EpisodiveNavigationBar {
                appState.bottomBarDestinations.forEach { destination ->
                    val selected = destination.navKey == appState.navigationState.topLevelRoute
                    val text = stringResource(destination.iconTextId)

                    EpisodiveNavigationBarItem(
                        icon = {
                            MorphIcon(
                                imageVector = if (selected) destination.selectedIcon else destination.unselectedIcon,
                                contentDescription = text
                            )
                        },
                        label = {
                            Text(
                                text = text,
                                // 원본 내비 라벨은 11px, 선택 700 / 비선택 500 (원본 줄 213·214).
                                // labelLarge(14/700)로 두면 라벨이 크고 선택 강조가 색에만 의존한다.
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                ),
                                maxLines = 1
                            )
                        },
                        selected = selected,
                        onClick = { appState.navigateToBottomBarDestination(destination) },
                    )
                }
            }
        },
        contentWindowInsets = ScaffoldDefaults.contentWindowInsets
            .exclude(WindowInsets.statusBars),
        snackbarHost = {
            // Scaffold 가 이미 스낵바를 하단 내비 위에 올려 주고, 그 내비가 시스템 인셋까지
            // 품고 있다. 여기서 safeDrawing 을 한 번 더 주면 인셋이 이중으로 들어가 스낵바가
            // 미니플레이어에서 한참 떠오른다. 미니플레이어가 차지하는 높이만 비우면 되고,
            // 둘 사이 간격은 SnackbarHost 가 기본으로 두는 12dp 여백이 만든다.
            EpisodiveSwipeDismissSnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .padding(bottom = LocalDimensionTheme.current.playerBarSpace),
            )
        },
    ) { paddingValues ->
        val dimension = LocalDimensionTheme.current

        // 미니플레이어가 들고 있는 현재 재생 에피소드를 화면 트리 전체에 내려, 어느 목록에서든
        // 재생 중인 항목 하나만 강조되게 한다 (에피소드 행의 레드 재생 버튼 + 진행률 링).
        var nowPlayingEpisodeId by remember { mutableStateOf<Long?>(null) }
        // 위 id 는 일시정지 중에도 남는다. 재생 여부까지 갈라야 하는 화면을 위해 따로 내린다.
        var isPlaying by remember { mutableStateOf(false) }

        CompositionLocalProvider(
            LocalNowPlayingEpisodeId provides nowPlayingEpisodeId,
            LocalIsPlaying provides isPlaying,
        ) {
            Box(
                modifier = Modifier
                    .padding(paddingValues)
            ) {
                // 미니플레이어는 화면 위에 떠 있다. 여기서 그 높이만큼 자리를 비워 두면
                // 그 띠가 불투명한 배경으로 남아 화면이 잘리므로 비우지 않는다. 마지막
                // 항목이 가리는 문제는 각 화면이 스크롤 끝에 playerBarSpace 를 두어 처리한다.
                EpisodiveNavHost(
                    navigationState = appState.navigationState,
                    navigator = appState.navigator,
                    onShowSnackbar = onShowSnackbar,
                    onSearchShortcutClick = {
                        // 열려 있던 플레이어 시트가 검색창을 가리지 않도록 접는다 —
                        // 팟캐스트로 넘어갈 때(L127)와 같은 이유다.
                        collapsePlayerSignal++
                        appState.navigateToSearchWithFocus()
                    },
                    searchAutoFocus = { appState.searchAutoFocus },
                    onSearchAutoFocusHandled = appState::consumeSearchAutoFocus,
                )

                PlayerBar(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = dimension.playerBarBottomMargin),
                    onPodcastClick = { appState.navigateToPodcast(it) },
                    onShowSnackbar = onShowSnackbar,
                    expandSignal = expandPlayerSignal,
                    collapseSignal = collapsePlayerSignal,
                    // 값이 아니라 읽는 함수로 넘긴다 — entryProvider 가 처음 캡처한 값에
                    // 갇히는 것과 같은 이유다(EpisodiveNavHost 주석 참고).
                    pendingEpisode = { appState.pendingEpisodeDeepLink },
                    onPendingEpisodeHandled = appState::consumeEpisodeDeepLink,
                    onNowPlayingChange = { nowPlayingEpisodeId = it },
                    onIsPlayingChange = { isPlaying = it },
                )
            }
        }
    }
}
