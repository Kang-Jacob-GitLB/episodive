package io.jacob.episodive.core.data.util.query

import io.jacob.episodive.core.model.Category
import io.jacob.episodive.core.model.GroupKey
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.minutes

sealed interface EpisodeQuery : CacheableQuery {

    data class Person(
        val person: String,
    ) : EpisodeQuery {
        override val key = "${GroupKey.PERSON}:$person"
        override val timeToLive = 30.minutes
    }

    data class FeedId(
        val feedId: Long,
    ) : EpisodeQuery {
        override val key = "${GroupKey.FEED_ID}:$feedId"
        override val timeToLive = 1.days
    }

    data class FeedUrl(
        val feedUrl: String,
    ) : EpisodeQuery {
        override val key = "${GroupKey.FEED_URL}:$feedUrl"
        override val timeToLive = 1.days
    }

    data class PodcastGuid(
        val podcastGuid: String,
    ) : EpisodeQuery {
        override val key = "${GroupKey.PODCAST_GUID}:$podcastGuid"
        override val timeToLive = 1.days
    }

    data object Live : EpisodeQuery {
        override val key = GroupKey.LIVE.toString()
        override val timeToLive = 10.minutes
    }

    data class Random(
        val language: String? = null,
        val categories: List<Category> = emptyList(),
    ) : EpisodeQuery {
        override val key = GroupKey.RANDOM.toString()
        override val timeToLive = 10.minutes
    }

    data object Recent : EpisodeQuery {
        override val key = GroupKey.RECENT.toString()
        override val timeToLive = 10.minutes
    }

    data object Soundbite : EpisodeQuery {
        override val key = GroupKey.SOUNDBITE.toString()
        override val timeToLive = 10.minutes
    }
}