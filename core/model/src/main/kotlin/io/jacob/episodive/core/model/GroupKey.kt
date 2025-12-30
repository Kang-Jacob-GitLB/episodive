package io.jacob.episodive.core.model

enum class GroupKey(val value: String) {
    FEED_ID("feedId"),
    FEED_URL("feedUrl"),
    FEED_GUID("feedGuid"),
    MEDIUM("medium"),
    CHANNEL("channel"),
    TRENDING("trending"),
    RECENT("recent"),
    RECENT_NEW("recentNew"),
    RECOMMENDED("recommended"),
    PERSON("person"),
    PODCAST_GUID("podcastGuid"),
    LIVE("live"),
    RANDOM("random"),
    SOUNDBITE("soundbite"),
    PLAYLIST("playlist"),
    ;

    override fun toString(): String = value
}