package io.jacob.episodive.core.database.mapper

import androidx.annotation.RestrictTo
import io.jacob.episodive.core.database.model.EpisodeDto
import io.jacob.episodive.core.database.model.EpisodeEntity
import io.jacob.episodive.core.database.model.PodcastDto
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
import kotlin.time.Duration
import kotlin.time.Instant

fun PodcastDto.toPodcast(): Podcast =
    Podcast(
        id = podcast.id,
        podcastGuid = podcast.podcastGuid,
        title = podcast.title,
        url = podcast.url,
        originalUrl = podcast.originalUrl,
        link = podcast.link,
        description = podcast.description,
        author = podcast.author,
        ownerName = podcast.ownerName,
        image = podcast.image,
        artwork = podcast.artwork,
        lastUpdateTime = podcast.lastUpdateTime,
        lastCrawlTime = podcast.lastCrawlTime,
        lastParseTime = podcast.lastParseTime,
        lastGoodHttpStatusTime = podcast.lastGoodHttpStatusTime,
        lastHttpStatus = podcast.lastHttpStatus,
        contentType = podcast.contentType,
        itunesId = podcast.itunesId,
        itunesType = podcast.itunesType,
        generator = podcast.generator,
        language = podcast.language,
        explicit = podcast.explicit,
        type = podcast.type,
        medium = podcast.medium,
        dead = podcast.dead,
        chash = podcast.chash,
        episodeCount = podcast.episodeCount,
        crawlErrors = podcast.crawlErrors,
        parseErrors = podcast.parseErrors,
        categories = podcast.categories,
        locked = podcast.locked,
        imageUrlHash = podcast.imageUrlHash,
        newestItemPublishTime = podcast.newestItemPublishTime,
        followedAt = followedAt,
        isNotificationEnabled = isNotificationEnabled ?: false,
    )

fun List<PodcastDto>.toPodcasts(): List<Podcast> =
    map { it.toPodcast() }

@RestrictTo(RestrictTo.Scope.TESTS)
fun Podcast.toPodcastDto(
    cacheKey: String,
    cachedAt: Instant = Clock.System.now(),
): PodcastDto =
    PodcastDto(
        podcast = toPodcastEntity(
            cacheKey = cacheKey,
            cachedAt = cachedAt,
        ),
        followedAt = followedAt,
        isNotificationEnabled = isNotificationEnabled,
    )

@RestrictTo(RestrictTo.Scope.TESTS)
fun List<Podcast>.toPodcastDtos(
    cacheKey: String,
    cachedAt: Instant = Clock.System.now(),
): List<PodcastDto> =
    map {
        it.toPodcastDto(
            cacheKey = cacheKey,
            cachedAt = cachedAt,
        )
    }

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
    cachedAt: Instant = Clock.System.now(),
): List<PodcastEntity> =
    map {
        it.toPodcastEntity(
            cacheKey = cacheKey,
            cachedAt = cachedAt,
        )
    }

fun EpisodeDto.toEpisode(): Episode =
    Episode(
        id = episode.id,
        guid = episode.guid,
        title = episode.title,
        link = episode.link,
        description = episode.description,
        datePublished = episode.datePublished,
        dateCrawled = episode.dateCrawled,
        enclosureUrl = episode.enclosureUrl,
        enclosureType = episode.enclosureType,
        enclosureLength = episode.enclosureLength,
        startTime = episode.startTime,
        endTime = episode.endTime,
        status = episode.status,
        contentLink = episode.contentLink,
        duration = episode.duration,
        explicit = episode.explicit,
        episode = episode.episode,
        episodeType = episode.episodeType,
        season = episode.season,
        image = episode.image,
        feedItunesId = episode.feedItunesId,
        feedImage = episode.feedImage,
        feedId = episode.feedId,
        feedUrl = episode.feedUrl,
        feedAuthor = episode.feedAuthor,
        feedTitle = episode.feedTitle,
        feedLanguage = episode.feedLanguage,
        categories = episode.categories,
        chaptersUrl = episode.chaptersUrl,
        transcriptUrl = episode.transcriptUrl,
        likedAt = likedAt,
        playedAt = playedAt,
        position = position ?: Duration.ZERO,
        isCompleted = isCompleted ?: false,
    )

fun List<EpisodeDto>.toEpisodes(): List<Episode> =
    map { it.toEpisode() }

@RestrictTo(RestrictTo.Scope.TESTS)
fun Episode.toEpisodeDto(
    cacheKey: String,
    cachedAt: Instant = Clock.System.now(),
): EpisodeDto =
    EpisodeDto(
        episode = toEpisodeEntity(
            cacheKey = cacheKey,
            cachedAt = cachedAt,
        ),
        likedAt = likedAt,
        playedAt = playedAt,
        position = position,
        isCompleted = isCompleted,
    )

@RestrictTo(RestrictTo.Scope.TESTS)
fun List<Episode>.toEpisodeDtos(
    cacheKey: String,
    cachedAt: Instant = Clock.System.now(),
): List<EpisodeDto> =
    map {
        it.toEpisodeDto(
            cacheKey = cacheKey,
            cachedAt = cachedAt,
        )
    }

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