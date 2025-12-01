package io.jacob.episodive.core.network.mapper

import androidx.annotation.RestrictTo
import io.jacob.episodive.core.model.Chapter
import io.jacob.episodive.core.model.Episode
import io.jacob.episodive.core.model.Podcast
import io.jacob.episodive.core.model.RecentFeed
import io.jacob.episodive.core.model.RecentNewFeed
import io.jacob.episodive.core.model.RecentNewValueFeed
import io.jacob.episodive.core.model.Soundbite
import io.jacob.episodive.core.model.TrendingFeed
import io.jacob.episodive.core.model.mapper.toCategories
import io.jacob.episodive.core.model.mapper.toDurationSeconds
import io.jacob.episodive.core.model.mapper.toEpisodeType
import io.jacob.episodive.core.model.mapper.toInstant
import io.jacob.episodive.core.model.mapper.toIntSeconds
import io.jacob.episodive.core.model.mapper.toMap
import io.jacob.episodive.core.model.mapper.toMedium
import io.jacob.episodive.core.model.mapper.toSeconds
import io.jacob.episodive.core.network.model.ChapterResponse
import io.jacob.episodive.core.network.model.ChaptersResponse
import io.jacob.episodive.core.network.model.EpisodeResponse
import io.jacob.episodive.core.network.model.PodcastResponse
import io.jacob.episodive.core.network.model.RecentFeedResponse
import io.jacob.episodive.core.network.model.RecentNewFeedResponse
import io.jacob.episodive.core.network.model.RecentNewValueFeedResponse
import io.jacob.episodive.core.network.model.SoundbiteResponse
import io.jacob.episodive.core.network.model.TrendingFeedResponse
import kotlin.time.Duration.Companion.seconds

fun PodcastResponse.toPodcast(): Podcast =
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
        lastUpdateTime = lastUpdateTime.toInstant(),
        lastCrawlTime = lastCrawlTime.toInstant(),
        lastParseTime = lastParseTime.toInstant(),
        lastGoodHttpStatusTime = lastGoodHttpStatusTime.toInstant(),
        lastHttpStatus = lastHttpStatus,
        contentType = contentType,
        itunesId = itunesId,
        itunesType = itunesType,
        generator = generator,
        language = language,
        explicit = explicit,
        type = type,
        medium = medium?.toMedium(),
        dead = dead,
        chash = chash,
        episodeCount = episodeCount,
        crawlErrors = crawlErrors,
        parseErrors = parseErrors,
        categories = categories?.toCategories() ?: emptyList(),
        locked = locked,
        imageUrlHash = imageUrlHash,
        newestItemPublishTime = newestItemPublishTime?.toInstant(),
    )

fun List<PodcastResponse>.toPodcasts(): List<Podcast> =
    map { it.toPodcast() }

@RestrictTo(RestrictTo.Scope.TESTS)
fun Podcast.toPodcastResponse(): PodcastResponse =
    PodcastResponse(
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
        lastUpdateTime = lastUpdateTime.toSeconds(),
        lastCrawlTime = lastCrawlTime.toSeconds(),
        lastParseTime = lastParseTime.toSeconds(),
        lastGoodHttpStatusTime = lastGoodHttpStatusTime.toSeconds(),
        lastHttpStatus = lastHttpStatus,
        contentType = contentType,
        itunesId = itunesId,
        itunesType = itunesType,
        generator = generator,
        language = language,
        explicit = explicit,
        type = type,
        medium = medium?.value,
        dead = dead,
        chash = chash,
        episodeCount = episodeCount,
        crawlErrors = crawlErrors,
        parseErrors = parseErrors,
        categories = categories.toMap(),
        locked = locked,
        imageUrlHash = imageUrlHash,
        newestItemPublishTime = newestItemPublishTime?.toSeconds(),
    )

@RestrictTo(RestrictTo.Scope.TESTS)
fun List<Podcast>.toPodcastResponses(): List<PodcastResponse> =
    map { it.toPodcastResponse() }

fun EpisodeResponse.toEpisode(): Episode =
    Episode(
        id = id,
        guid = guid,
        title = title,
        link = link,
        description = description,
        datePublished = datePublished.toInstant(),
        dateCrawled = dateCrawled.toInstant(),
        enclosureUrl = enclosureUrl,
        enclosureType = enclosureType,
        enclosureLength = enclosureLength,
        startTime = startTime?.toInstant(),
        endTime = endTime?.toInstant(),
        status = status,
        contentLink = contentLink,
        duration = duration?.toDurationSeconds(),
        explicit = explicit == 1,
        episode = episode,
        episodeType = episodeType?.toEpisodeType(),
        season = season,
        image = image,
        feedItunesId = feedItunesId,
        feedImage = feedImage,
        feedId = feedId,
        feedUrl = feedUrl,
        feedAuthor = feedAuthor,
        feedTitle = feedTitle,
        feedLanguage = feedLanguage,
        categories = categories?.toCategories() ?: emptyList(),
        chaptersUrl = chaptersUrl,
        transcriptUrl = transcriptUrl,
    )

fun List<EpisodeResponse>.toEpisodes(): List<Episode> =
    map { it.toEpisode() }

@RestrictTo(RestrictTo.Scope.TESTS)
fun Episode.toEpisodeResponse(): EpisodeResponse =
    EpisodeResponse(
        id = id,
        guid = guid,
        title = title,
        link = link,
        description = description,
        datePublished = datePublished.toSeconds(),
        dateCrawled = dateCrawled.toSeconds(),
        enclosureUrl = enclosureUrl,
        enclosureType = enclosureType,
        enclosureLength = enclosureLength,
        startTime = startTime?.toSeconds(),
        endTime = endTime?.toSeconds(),
        status = status,
        contentLink = contentLink,
        duration = duration?.toIntSeconds(),
        explicit = if (explicit) 1 else 0,
        episode = episode,
        episodeType = episodeType?.value,
        season = season,
        image = image,
        feedItunesId = feedItunesId,
        feedImage = feedImage,
        feedId = feedId,
        feedUrl = feedUrl,
        feedAuthor = feedAuthor,
        feedTitle = feedTitle,
        feedLanguage = feedLanguage,
        categories = categories.toMap(),
        chaptersUrl = chaptersUrl,
        transcriptUrl = transcriptUrl,
    )

@RestrictTo(RestrictTo.Scope.TESTS)
fun List<Episode>.toEpisodeResponses(): List<EpisodeResponse> =
    map { it.toEpisodeResponse() }

fun TrendingFeedResponse.toTrendingFeed(): TrendingFeed =
    TrendingFeed(
        id = id,
        url = url,
        title = title,
        description = description,
        author = author,
        image = image,
        artwork = artwork,
        newestItemPublishTime = newestItemPublishTime.toInstant(),
        itunesId = itunesId,
        trendScore = trendScore,
        language = language,
        categories = categories?.toCategories() ?: emptyList(),
    )

fun List<TrendingFeedResponse>.toTrendingFeeds(): List<TrendingFeed> =
    map { it.toTrendingFeed() }

@RestrictTo(RestrictTo.Scope.TESTS)
fun TrendingFeed.toTrendingFeedResponse(): TrendingFeedResponse =
    TrendingFeedResponse(
        id = id,
        url = url,
        title = title,
        description = description,
        author = author,
        image = image,
        artwork = artwork,
        newestItemPublishTime = newestItemPublishTime.toSeconds(),
        itunesId = itunesId,
        trendScore = trendScore,
        language = language,
        categories = categories.toMap(),
    )

@RestrictTo(RestrictTo.Scope.TESTS)
fun List<TrendingFeed>.toTrendingFeedResponses(): List<TrendingFeedResponse> =
    map { it.toTrendingFeedResponse() }

fun RecentFeedResponse.toRecentFeed(): RecentFeed =
    RecentFeed(
        id = id,
        url = url,
        title = title,
        newestItemPublishTime = newestItemPublishTime.toInstant(),
        oldestItemPublishTime = oldestItemPublishTime?.toInstant(),
        description = description,
        author = author,
        image = image,
        itunesId = itunesId,
        trendScore = trendScore,
        language = language,
        categories = categories?.toCategories() ?: emptyList(),
    )

fun List<RecentFeedResponse>.toRecentFeeds(): List<RecentFeed> =
    map { it.toRecentFeed() }

@RestrictTo(RestrictTo.Scope.TESTS)
fun RecentFeed.toRecentFeedResponse(): RecentFeedResponse =
    RecentFeedResponse(
        id = id,
        url = url,
        title = title,
        newestItemPublishTime = newestItemPublishTime.toSeconds(),
        oldestItemPublishTime = oldestItemPublishTime?.toSeconds(),
        description = description,
        author = author,
        image = image,
        itunesId = itunesId,
        trendScore = trendScore,
        language = language,
        categories = categories.toMap(),
    )

@RestrictTo(RestrictTo.Scope.TESTS)
fun List<RecentFeed>.toRecentFeedResponses(): List<RecentFeedResponse> =
    map { it.toRecentFeedResponse() }

fun RecentNewFeedResponse.toRecentNewFeed(): RecentNewFeed =
    RecentNewFeed(
        id = id,
        url = url,
        timeAdded = timeAdded.toInstant(),
        status = status,
        contentHash = contentHash,
        language = language,
    )

fun List<RecentNewFeedResponse>.toRecentNewFeeds(): List<RecentNewFeed> =
    map { it.toRecentNewFeed() }

@RestrictTo(RestrictTo.Scope.TESTS)
fun RecentNewFeed.toRecentNewFeedResponse(): RecentNewFeedResponse =
    RecentNewFeedResponse(
        id = id,
        url = url,
        timeAdded = timeAdded.toSeconds(),
        status = status,
        contentHash = contentHash,
        language = language,
    )

@RestrictTo(RestrictTo.Scope.TESTS)
fun List<RecentNewFeed>.toRecentNewFeedResponses(): List<RecentNewFeedResponse> =
    map { it.toRecentNewFeedResponse() }

fun RecentNewValueFeedResponse.toRecentNewValueFeed(): RecentNewValueFeed =
    RecentNewValueFeed(
        id = id,
        url = url,
        title = title,
        author = author,
        image = image,
        newestItemPublishTime = newestItemPublishTime.toInstant(),
        itunesId = itunesId,
        trendScore = trendScore,
        language = language,
        categories = categories?.toCategories() ?: emptyList(),
    )

fun List<RecentNewValueFeedResponse>.toRecentNewValueFeeds(): List<RecentNewValueFeed> =
    map { it.toRecentNewValueFeed() }

@RestrictTo(RestrictTo.Scope.TESTS)
fun RecentNewValueFeed.toRecentNewValueFeedResponse(): RecentNewValueFeedResponse =
    RecentNewValueFeedResponse(
        id = id,
        url = url,
        title = title,
        author = author,
        image = image,
        newestItemPublishTime = newestItemPublishTime.toSeconds(),
        itunesId = itunesId,
        trendScore = trendScore,
        language = language,
        categories = categories.toMap(),
    )

@RestrictTo(RestrictTo.Scope.TESTS)
fun List<RecentNewValueFeed>.toRecentNewValueFeedResponses(): List<RecentNewValueFeedResponse> =
    map { it.toRecentNewValueFeedResponse() }

fun SoundbiteResponse.toSoundbite(): Soundbite =
    Soundbite(
        enclosureUrl = enclosureUrl,
        title = title,
        startTime = startTime.toInstant(),
        duration = duration.toDurationSeconds(),
        episodeId = episodeId,
        episodeTitle = episodeTitle,
        feedTitle = feedTitle,
        feedUrl = feedUrl,
        feedId = feedId,
    )

fun List<SoundbiteResponse>.toSoundbites(): List<Soundbite> =
    map { it.toSoundbite() }

@RestrictTo(RestrictTo.Scope.TESTS)
fun Soundbite.toSoundbiteResponse(): SoundbiteResponse =
    SoundbiteResponse(
        enclosureUrl = enclosureUrl,
        title = title,
        startTime = startTime.toSeconds(),
        duration = duration.toIntSeconds(),
        episodeId = episodeId,
        episodeTitle = episodeTitle,
        feedTitle = feedTitle,
        feedUrl = feedUrl,
        feedId = feedId,
    )

fun ChapterResponse.toChapter(): Chapter =
    Chapter(
        title = title,
        startTime = startTime.seconds,
        endTime = endTime.seconds,
    )

fun ChaptersResponse.toChapters(): List<Chapter> =
    chapters.map { it.toChapter() }