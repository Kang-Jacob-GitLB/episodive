package io.jacob.episodive.navigation

import androidx.navigation3.runtime.NavKey

class EpisodiveNavigator(val state: EpisodiveNavigationState) {

    fun navigate(route: NavKey) {
        if (route in state.backStacks.keys) {
            if (route != state.topLevelRoute) state.isTabNavigation = true
            state.topLevelRoute = route
        } else {
            state.isTabNavigation = false
            state.backStacks[state.topLevelRoute]?.add(route)
        }
    }

    fun goBack() {
        val currentStack = state.backStacks[state.topLevelRoute]
            ?: error("Stack for ${state.topLevelRoute} not found")
        val currentRoute = currentStack.last()

        state.isTabNavigation = false
        if (currentRoute == state.topLevelRoute) {
            state.topLevelRoute = state.startRoute
        } else {
            currentStack.removeLastOrNull()
        }
    }

    fun navigateToTabRoot() {
        val currentStack = state.backStacks[state.topLevelRoute] ?: return
        if (currentStack.size <= 1) return
        state.isTabNavigation = true
        while (currentStack.size > 1) {
            currentStack.removeLastOrNull()
        }
    }

    /**
     * 현재 탭과 무관하게 시작 탭(홈) 스택에서 [route] 를 연다. 이미 스택에 있으면 그 위를 걷어 내고,
     * 없으면 쌓는다.
     *
     * 화면은 시작 탭 스택과 현재 탭 스택을 이어 그리므로, 같은 data object 키를 두 스택에 올리면 중복
     * 키로 크래시할 수 있다. 전역에서 여는 화면(라디오)은 이 경로로 시작 탭 스택에만 올린다.
     */
    fun navigateInStartStack(route: NavKey) {
        val stack = state.backStacks[state.startRoute] ?: return
        state.isTabNavigation = true
        state.topLevelRoute = state.startRoute

        val index = stack.indexOf(route)
        if (index == -1) {
            stack.add(route)
        } else {
            while (stack.size > index + 1) {
                stack.removeLastOrNull()
            }
        }
    }
}
