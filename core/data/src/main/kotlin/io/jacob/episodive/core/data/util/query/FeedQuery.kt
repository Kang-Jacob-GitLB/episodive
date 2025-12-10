package io.jacob.episodive.core.data.util.query

import io.jacob.episodive.core.model.Category
import io.jacob.episodive.core.model.mapper.toCommaString
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

sealed interface FeedQuery : CacheableQuery {

    data class Trending(
        val max: Int = 10,
        val language: String? = null,
        val categories: List<Category> = emptyList(),
    ) : FeedQuery {
        override val key: String = "trending:${language ?: "all"}:${categories.toCommaString()}"
        override val timeToLive: Duration = 1.hours
    }

    data class Recent(
        val max: Int = 10,
        val language: String? = null,
        val categories: List<Category> = emptyList(),
    ) : FeedQuery {
        override val key: String = "recent:${language ?: "all"}:${categories.toCommaString()}"
        override val timeToLive: Duration = 1.hours
    }

    data class RecentNew(val max: Int = 10) : FeedQuery {
        override val key: String = "recent_new"
        override val timeToLive: Duration = 1.hours
    }

    data class Soundbite(val max: Int = 10) : FeedQuery {
        override val key: String = "soundbite"
        override val timeToLive: Duration = 5.minutes
    }
}