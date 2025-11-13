package io.jacob.episodive.core.model

import java.util.Locale

data class UserData(
    val isFirstLaunch: Boolean = true,
    val language: String = Locale.getDefault().language,
    val categories: List<Category> = emptyList(),
    val speed: Float = 1f,
)
