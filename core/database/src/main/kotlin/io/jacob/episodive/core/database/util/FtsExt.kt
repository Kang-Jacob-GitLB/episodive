package io.jacob.episodive.core.database.util

fun String.asFtsWildcard(): String? {
    return this.trim()
        .takeIf { it.isNotBlank() }
        ?.let { "*$it*" }
}