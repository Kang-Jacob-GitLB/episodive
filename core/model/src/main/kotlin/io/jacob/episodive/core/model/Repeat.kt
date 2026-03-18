package io.jacob.episodive.core.model

enum class Repeat(val value: Int) {
    OFF(0), ONE(1), ALL(2);

    companion object {
        fun fromValue(value: Int) = entries.firstOrNull { it.value == value } ?: OFF
    }
}