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
}
