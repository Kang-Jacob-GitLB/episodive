package io.jacob.episodive.core.data.util.query

import io.jacob.episodive.core.model.Category
import io.jacob.episodive.core.model.Channel
import io.jacob.episodive.core.model.GroupKey
import io.jacob.episodive.core.model.mapper.toCommaString
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours

sealed interface PodcastQuery : CacheableQuery {

    data class FeedId(val feedId: Long) : PodcastQuery {
        override val key = "${GroupKey.FEED_ID}:$feedId"
        override val timeToLive = 1.hours
    }

    data class FeedUrl(val feedUrl: String) : PodcastQuery {
        override val key = "${GroupKey.FEED_URL}:$feedUrl"
        override val timeToLive = 1.hours
    }

    data class FeedGuid(val feedGuid: String) : PodcastQuery {
        override val key = "${GroupKey.FEED_GUID}:$feedGuid"
        override val timeToLive = 1.hours
    }

    data class Medium(val medium: String) : PodcastQuery {
        override val key = "${GroupKey.MEDIUM}:$medium"
        override val timeToLive = 1.hours
    }

    data class ByChannel(val channel: Channel) : PodcastQuery {
        override val key = "${GroupKey.CHANNEL}:${channel.id}"
        override val timeToLive = 7.days
    }

    data class Trending(
        val max: Int,
        val language: String? = null,
        val categories: List<Category> = emptyList(),
    ) : PodcastQuery {
        override val key: String =
            "${GroupKey.TRENDING}:${language ?: "all"}:${categories.toCommaString()}"
        override val timeToLive: Duration = 1.hours
    }

    data class Recent(
        val max: Int,
        val language: String? = null,
        val categories: List<Category> = emptyList(),
    ) : PodcastQuery {
        override val key: String =
            "${GroupKey.RECENT}:${language ?: "all"}:${categories.toCommaString()}"
        override val timeToLive: Duration = 1.hours
    }

    data class RecentNew(val max: Int) : PodcastQuery {
        override val key: String = GroupKey.RECENT_NEW.toString()
        override val timeToLive: Duration = 1.hours
    }

    data class Recommended(
        val max: Int,
        val language: String? = null,
        val categories: List<Category> = emptyList(),
    ) : PodcastQuery {
        override val key: String =
            "${GroupKey.RECOMMENDED}:${language ?: "all"}:${categories.toCommaString()}"
        override val timeToLive: Duration = 1.hours
    }
}