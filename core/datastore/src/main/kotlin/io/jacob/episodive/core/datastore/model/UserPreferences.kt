package io.jacob.episodive.core.datastore.model

import io.jacob.episodive.core.model.Category

data class UserPreferences(
    val isFirstLaunch: Boolean,
    val language: String,
    val categories: List<Category>,
    val speed: Float,
)