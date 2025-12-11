package io.jacob.episodive.core.data.util.query

import io.jacob.episodive.core.model.Channel
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

sealed interface PodcastQuery : CacheableQuery {

    data class Search(val query: String) : PodcastQuery {
        override val key = "search:$query"
        override val timeToLive = 30.minutes
    }

    data class FeedId(val feedId: Long) : PodcastQuery {
        override val key = "feedId:$feedId"
        override val timeToLive = 1.hours
    }

    data class FeedUrl(val feedUrl: String) : PodcastQuery {
        override val key = "feedUrl:$feedUrl"
        override val timeToLive = 1.hours
    }

    data class FeedGuid(val feedGuid: String) : PodcastQuery {
        override val key = "feedGuid:$feedGuid"
        override val timeToLive = 1.hours
    }

    data class Medium(val medium: String) : PodcastQuery {
        override val key = "medium:$medium"
        override val timeToLive = 1.hours
    }

    data class ByChannel(val channel: Channel) : PodcastQuery {
        override val key = "channel:${channel.id}"
        override val timeToLive = 7.days
    }
}