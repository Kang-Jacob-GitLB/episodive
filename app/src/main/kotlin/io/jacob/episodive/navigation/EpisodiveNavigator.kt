package io.jacob.episodive.navigation

import androidx.navigation3.runtime.NavKey

class EpisodiveNavigator(val state: EpisodiveNavigationState) {

    fun navigate(route: NavKey) {
        state.isTabNavigation = route in state.backStacks.keys
        if (state.isTabNavigation) {
            state.topLevelRoute = route
        } else {
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
        state.isTabNavigation = true
        while (currentStack.size > 1) {
            currentStack.removeLastOrNull()
        }
    }
}
