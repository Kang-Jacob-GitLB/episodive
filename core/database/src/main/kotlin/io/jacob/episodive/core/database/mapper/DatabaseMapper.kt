package io.jacob.episodive.core.database.mapper

import io.jacob.episodive.core.database.model.EpisodeEntity
import io.jacob.episodive.core.database.model.PodcastEntity
import io.jacob.episodive.core.database.model.RecentFeedEntity
import io.jacob.episodive.core.database.model.RecentNewFeedEntity
import io.jacob.episodive.core.database.model.SoundbiteEntity
import io.jacob.episodive.core.database.model.TrendingFeedEntity
import io.jacob.episodive.core.model.Episode
import io.jacob.episodive.core.model.Podcast
import io.jacob.episodive.core.model.RecentFeed
import io.jacob.episodive.core.model.RecentNewFeed
import io.jacob.episodive.core.model.Soundbite
import io.jacob.episodive.core.model.TrendingFeed
import kotlin.time.Clock
import kotlin.time.Instant

fun PodcastEntity.toPodcast(): Podcast =
    Podcast(
        id = id,
        podcastGuid = podcastGuid,
        title = title,
        url = url,
        originalUrl = originalUrl,
        link = link,
        description = description,
        author = author,
        ownerName = ownerName,
        image = image,
        artwork = artwork,
        lastUpdateTime = lastUpdateTime,
        lastCrawlTime = lastCrawlTime,
        lastParseTime = lastParseTime,
        lastGoodHttpStatusTime = lastGoodHttpStatusTime,
        lastHttpStatus = lastHttpStatus,
        contentType = contentType,
        itunesId = itunesId,
        itunesType = itunesType,
        generator = generator,
        language = language,
        explicit = explicit,
        type = type,
        medium = medium,
        dead = dead,
        chash = chash,
        episodeCount = episodeCount,
        crawlErrors = crawlErrors,
        parseErrors = parseErrors,
        categories = categories,
        locked = locked,
        imageUrlHash = imageUrlHash,
        newestItemPublishTime = newestItemPublishTime,
    )

fun List<PodcastEntity>.toPodcasts(): List<Podcast> =
    map { it.toPodcast() }

fun Podcast.toPodcastEntity(
    cacheKey: String,
    cachedAt: Instant = Clock.System.now(),
): PodcastEntity =
    PodcastEntity(
        id = id,
        podcastGuid = podcastGuid,
        title = title,
        url = url,
        originalUrl = originalUrl,
        link = link,
        description = description,
        author = author,
        ownerName = ownerName,
        image = image,
        artwork = artwork,
        lastUpdateTime = lastUpdateTime,
        lastCrawlTime = lastCrawlTime,
        lastParseTime = lastParseTime,
        lastGoodHttpStatusTime = lastGoodHttpStatusTime,
        lastHttpStatus = lastHttpStatus,
        contentType = contentType,
        itunesId = itunesId,
        itunesType = itunesType,
        generator = generator,
        language = language,
        explicit = explicit,
        type = type,
        medium = medium,
        dead = dead,
        chash = chash,
        episodeCount = episodeCount,
        crawlErrors = crawlErrors,
        parseErrors = parseErrors,
        categories = categories,
        locked = locked,
        imageUrlHash = imageUrlHash,
        newestItemPublishTime = newestItemPublishTime,
        cacheKey = cacheKey,
        cachedAt = cachedAt,
    )

fun List<Podcast>.toPodcastEntities(
    cacheKey: String,
    cachedAt: Instant = Clock.System.now()
): List<PodcastEntity> =
    map {
        it.toPodcastEntity(
            cacheKey = cacheKey,
            cachedAt = cachedAt,
        )
    }

fun EpisodeEntity.toEpisode(): Episode =
    Episode(
        id = id,
        guid = guid,
        title = title,
        link = link,
        description = description,
        datePublished = datePublished,
        dateCrawled = dateCrawled,
        enclosureUrl = enclosureUrl,
        enclosureType = enclosureType,
        enclosureLength = enclosureLength,
        startTime = startTime,
        endTime = endTime,
        status = status,
        contentLink = contentLink,
        duration = duration,
        explicit = explicit,
        episode = episode,
        episodeType = episodeType,
        season = season,
        image = image,
        feedItunesId = feedItunesId,
        feedImage = feedImage,
        feedId = feedId,
        feedUrl = feedUrl,
        feedAuthor = feedAuthor,
        feedTitle = feedTitle,
        feedLanguage = feedLanguage,
        categories = categories,
        chaptersUrl = chaptersUrl,
        transcriptUrl = transcriptUrl,
        transcripts = transcripts,
    )

fun List<EpisodeEntity>.toEpisodes(): List<Episode> =
    map { it.toEpisode() }

fun Episode.toEpisodeEntity(
    cacheKey: String,
    cachedAt: Instant = Clock.System.now(),
): EpisodeEntity =
    EpisodeEntity(
        id = id,
        guid = guid,
        title = title,
        link = link,
        description = description,
        datePublished = datePublished,
        dateCrawled = dateCrawled,
        enclosureUrl = enclosureUrl,
        enclosureType = enclosureType,
        enclosureLength = enclosureLength,
        startTime = startTime,
        endTime = endTime,
        status = status,
        contentLink = contentLink,
        duration = duration,
        explicit = explicit,
        episode = episode,
        episodeType = episodeType,
        season = season,
        image = image,
        feedItunesId = feedItunesId,
        feedImage = feedImage,
        feedId = feedId,
        feedUrl = feedUrl,
        feedAuthor = feedAuthor,
        feedTitle = feedTitle,
        feedLanguage = feedLanguage,
        categories = categories,
        chaptersUrl = chaptersUrl,
        transcriptUrl = transcriptUrl,
        transcripts = transcripts,
        cacheKey = cacheKey,
        cachedAt = cachedAt,
    )

fun List<Episode>.toEpisodeEntities(
    cacheKey: String,
    cachedAt: Instant = Clock.System.now(),
): List<EpisodeEntity> =
    map {
        it.toEpisodeEntity(
            cacheKey = cacheKey,
            cachedAt = cachedAt,
        )
    }

fun TrendingFeed.toTrendingFeedEntity(
    cacheKey: String,
    cachedAt: Instant = Clock.System.now(),
): TrendingFeedEntity =
    TrendingFeedEntity(
        id = id,
        url = url,
        title = title,
        description = description,
        author = author,
        image = image,
        artwork = artwork,
        newestItemPublishTime = newestItemPublishTime,
        itunesId = itunesId,
        trendScore = trendScore,
        language = language,
        categories = categories,
        cacheKey = cacheKey,
        cachedAt = cachedAt,
    )

fun List<TrendingFeed>.toTrendingFeedEntities(
    cacheKey: String,
    cachedAt: Instant = Clock.System.now(),
): List<TrendingFeedEntity> =
    map {
        it.toTrendingFeedEntity(
            cacheKey = cacheKey,
            cachedAt = cachedAt,
        )
    }

fun TrendingFeedEntity.toTrendingFeed(): TrendingFeed =
    TrendingFeed(
        id = id,
        url = url,
        title = title,
        description = description,
        author = author,
        image = image,
        artwork = artwork,
        newestItemPublishTime = newestItemPublishTime,
        itunesId = itunesId,
        trendScore = trendScore,
        language = language,
        categories = categories,
    )

fun List<TrendingFeedEntity>.toTrendingFeeds(): List<TrendingFeed> =
    map { it.toTrendingFeed() }

fun RecentFeed.toRecentFeedEntity(
    cacheKey: String,
    cachedAt: Instant = Clock.System.now(),
): RecentFeedEntity =
    RecentFeedEntity(
        id = id,
        url = url,
        title = title,
        newestItemPublishTime = newestItemPublishTime,
        oldestItemPublishTime = oldestItemPublishTime,
        description = description,
        author = author,
        image = image,
        itunesId = itunesId,
        trendScore = trendScore,
        language = language,
        categories = categories,
        cacheKey = cacheKey,
        cachedAt = cachedAt,
    )

fun List<RecentFeed>.toRecentFeedEntities(
    cacheKey: String,
    cachedAt: Instant = Clock.System.now(),
): List<RecentFeedEntity> =
    map {
        it.toRecentFeedEntity(
            cacheKey = cacheKey,
            cachedAt = cachedAt,
        )
    }

fun RecentFeedEntity.toRecentFeed(): RecentFeed =
    RecentFeed(
        id = id,
        url = url,
        title = title,
        newestItemPublishTime = newestItemPublishTime,
        oldestItemPublishTime = oldestItemPublishTime,
        description = description,
        author = author,
        image = image,
        itunesId = itunesId,
        trendScore = trendScore,
        language = language,
        categories = categories,
    )

fun List<RecentFeedEntity>.toRecentFeeds(): List<RecentFeed> =
    map { it.toRecentFeed() }

fun RecentNewFeed.toRecentNewFeedEntity(
    cacheKey: String,
    cachedAt: Instant = Clock.System.now(),
): RecentNewFeedEntity =
    RecentNewFeedEntity(
        id = id,
        url = url,
        timeAdded = timeAdded,
        status = status,
        contentHash = contentHash,
        language = language,
        cacheKey = cacheKey,
        cachedAt = cachedAt,
    )

fun List<RecentNewFeed>.toRecentNewFeedEntities(
    cacheKey: String,
    cachedAt: Instant = Clock.System.now(),
): List<RecentNewFeedEntity> =
    map {
        it.toRecentNewFeedEntity(
            cacheKey = cacheKey,
            cachedAt = cachedAt,
        )
    }

fun RecentNewFeedEntity.toRecentNewFeed(): RecentNewFeed =
    RecentNewFeed(
        id = id,
        url = url,
        timeAdded = timeAdded,
        status = status,
        contentHash = contentHash,
        language = language,
    )

fun List<RecentNewFeedEntity>.toRecentNewFeeds(): List<RecentNewFeed> =
    map { it.toRecentNewFeed() }

fun Soundbite.toSoundbiteEntity(
    cacheKey: String,
    cachedAt: Instant = Clock.System.now(),
): SoundbiteEntity =
    SoundbiteEntity(
        enclosureUrl = enclosureUrl,
        title = title,
        startTime = startTime,
        duration = duration,
        episodeId = episodeId,
        episodeTitle = episodeTitle,
        feedTitle = feedTitle,
        feedUrl = feedUrl,
        feedId = feedId,
        cacheKey = cacheKey,
        cachedAt = cachedAt,
    )

fun List<Soundbite>.toSoundbiteEntities(
    cacheKey: String,
    cachedAt: Instant = Clock.System.now(),
): List<SoundbiteEntity> =
    map {
        it.toSoundbiteEntity(
            cacheKey = cacheKey,
            cachedAt = cachedAt,
        )
    }

fun SoundbiteEntity.toSoundbite(): Soundbite =
    Soundbite(
        enclosureUrl = enclosureUrl,
        title = title,
        startTime = startTime,
        duration = duration,
        episodeId = episodeId,
        episodeTitle = episodeTitle,
        feedTitle = feedTitle,
        feedUrl = feedUrl,
        feedId = feedId,
    )

fun List<SoundbiteEntity>.toSoundbites(): List<Soundbite> =
    map { it.toSoundbite() }