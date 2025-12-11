package io.jacob.episodive.core.data.util.query

import io.jacob.episodive.core.model.Category
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.minutes

sealed interface EpisodeQuery : CacheableQuery {

    data class Person(
        val person: String,
    ) : EpisodeQuery {
        override val key = "person:$person"
        override val timeToLive = 30.minutes
    }

    data class FeedId(
        val feedId: Long,
    ) : EpisodeQuery {
        override val key = "feedId:$feedId"
        override val timeToLive = 1.days
    }

    data class FeedUrl(
        val feedUrl: String,
    ) : EpisodeQuery {
        override val key = "feedUrl:$feedUrl"
        override val timeToLive = 1.days
    }

    data class PodcastGuid(
        val podcastGuid: String,
    ) : EpisodeQuery {
        override val key = "podcastGuid:$podcastGuid"
        override val timeToLive = 1.days
    }

    data object Live : EpisodeQuery {
        override val key = "live"
        override val timeToLive = 10.minutes
    }

    data class Random(
        val language: String? = null,
        val categories: List<Category> = emptyList(),
    ) : EpisodeQuery {
        override val key = "random"
        override val timeToLive = 10.minutes
    }

    data object Recent : EpisodeQuery {
        override val key = "recent"
        override val timeToLive = 10.minutes
    }

    data class EpisodeId(val episodeId: Long) : EpisodeQuery {
        override val key = "episodeId:$episodeId"
        override val timeToLive = 1.days
    }
}