package io.jacob.episodive.core.data.util.query

import io.jacob.episodive.core.model.Category
import io.jacob.episodive.core.model.mapper.toCommaString
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

sealed interface FeedQuery : CacheableQuery {

    data class Trending(
        val language: String? = null,
        val categories: List<Category> = emptyList(),
    ) : FeedQuery {
        override val key: String = "trending:${language ?: "all"}:${categories.toCommaString()}"
        override val timeToLive: Duration = 1.hours
    }

    data class Recent(
        val language: String? = null,
        val categories: List<Category> = emptyList(),
    ) : FeedQuery {
        override val key: String = "recent:${language ?: "all"}:${categories.toCommaString()}"
        override val timeToLive: Duration = 1.hours
    }

    data object RecentNew : FeedQuery {
        override val key: String = "recent_new"
        override val timeToLive: Duration = 1.hours
    }

    data object Soundbite : FeedQuery {
        override val key: String = "soundbite"
        override val timeToLive: Duration = 5.minutes
    }
}