package io.jacob.episodive.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSerializable
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberDecoratedNavEntries
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.runtime.serialization.NavKeySerializer
import androidx.savedstate.compose.serialization.serializers.MutableStateSerializer

@Composable
fun rememberEpisodiveNavigationState(
    startRoute: NavKey,
    topLevelRoutes: Set<NavKey>,
): EpisodiveNavigationState {
    val topLevelRoute = rememberSerializable(
        startRoute, topLevelRoutes,
        serializer = MutableStateSerializer(NavKeySerializer()),
    ) {
        mutableStateOf(startRoute)
    }

    val backStacks = topLevelRoutes.associateWith { key -> rememberNavBackStack(key) }

    return remember(startRoute, topLevelRoutes) {
        EpisodiveNavigationState(
            startRoute = startRoute,
            topLevelRoute = topLevelRoute,
            backStacks = backStacks,
        )
    }
}

class EpisodiveNavigationState(
    val startRoute: NavKey,
    topLevelRoute: MutableState<NavKey>,
    val backStacks: Map<NavKey, NavBackStack<NavKey>>,
) {
    var topLevelRoute: NavKey by topLevelRoute

    /**
     * 마지막 이동이 탭 조작(탭 선택·같은 탭 재선택)이었는가. pop 전환이 뒤로가기인지 탭
     * 전환인지 가르는 데 쓴다(`tabAwarePopTransitionSpec`). 판정은 [EpisodiveNavigator] 가
     * 스택을 실제로 바꾸는 조작 직전에 한다 — 스택은 반드시 navigator 를 거쳐 바꿔야 이 값이 맞다.
     *
     * 스냅샷 상태가 아니다. 전환 스펙은 스택이 바뀐 뒤의 재구성에서 한 번 읽을 뿐이라, 이 값이
     * 바뀐다고 다시 그릴 것이 없다. 탭 루트에서 뒤로가기로 홈에 돌아가는 것은 탭이 바뀌어도
     * 뒤로가기다 — 제스처로 하면 미리보기가 돌므로 버튼도 그에 맞춘다.
     */
    var isTabNavigation: Boolean = false
        internal set

    @Composable
    fun toDecoratedEntries(
        entryProvider: (NavKey) -> NavEntry<NavKey>,
    ): List<NavEntry<NavKey>> {
        val decoratedEntries = backStacks.mapValues { (_, stack) ->
            val decorators = listOf(
                rememberSaveableStateHolderNavEntryDecorator<NavKey>(),
                rememberViewModelStoreNavEntryDecorator(),
                OpaqueBackgroundNavEntryDecorator,
            )
            rememberDecoratedNavEntries(
                backStack = stack,
                entryDecorators = decorators,
                entryProvider = entryProvider,
            )
        }

        return getTopLevelRoutesInUse()
            .flatMap { decoratedEntries[it] ?: emptyList() }
    }

    private fun getTopLevelRoutesInUse(): List<NavKey> =
        if (topLevelRoute == startRoute) {
            listOf(startRoute)
        } else {
            listOf(startRoute, topLevelRoute)
        }
}
