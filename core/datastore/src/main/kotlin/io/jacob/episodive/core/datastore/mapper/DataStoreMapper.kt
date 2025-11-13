package io.jacob.episodive.core.datastore.mapper

import io.jacob.episodive.core.datastore.model.UserPreferences
import io.jacob.episodive.core.model.UserData

fun UserPreferences.toUserData(): UserData =
    UserData(
        isFirstLaunch = isFirstLaunch,
        language = language,
        categories = categories,
        speed = speed,
    )

fun UserData.toUserPreferences(): UserPreferences =
    UserPreferences(
        isFirstLaunch = isFirstLaunch,
        language = language,
        categories = categories,
        speed = speed,
    )