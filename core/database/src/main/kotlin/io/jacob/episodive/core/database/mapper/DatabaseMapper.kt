package io.jacob.episodive.core.database.mapper

import androidx.annotation.RestrictTo
import io.jacob.episodive.core.database.model.EpisodeEntity
import io.jacob.episodive.core.database.model.EpisodeWithExtrasView
import io.jacob.episodive.core.database.model.PodcastEntity
import io.jacob.episodive.core.database.model.PodcastWithExtrasView
import io.jacob.episodive.core.database.model.SoundbiteEntity
import io.jacob.episodive.core.model.Episode
import io.jacob.episodive.core.model.Podcast
import io.jacob.episodive.core.model.Soundbite
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Instant

fun PodcastWithExtrasView.toPodcast(): Podcast =
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

fun List<PodcastWithExtrasView>.toPodcasts(): List<Podcast> =
    map { it.toPodcast() }

@RestrictTo(RestrictTo.Scope.TESTS)
fun Podcast.toPodcastWithExtrasView(): PodcastWithExtrasView =
    PodcastWithExtrasView(
        podcast = toPodcastEntity(),
        followedAt = followedAt,
        isNotificationEnabled = isNotificationEnabled,
    )

@RestrictTo(RestrictTo.Scope.TESTS)
fun List<Podcast>.toPodcastWithExtrasViews(): List<PodcastWithExtrasView> =
    map { it.toPodcastWithExtrasView() }

fun Podcast.toPodcastEntity(): PodcastEntity =
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
    )

fun List<Podcast>.toPodcastEntities(): List<PodcastEntity> =
    map { it.toPodcastEntity() }

fun EpisodeWithExtrasView.toEpisode(): Episode =
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
        clipStartTime = clipStartTime,
        clipDuration = clipDuration,
    )

fun List<EpisodeWithExtrasView>.toEpisodes(): List<Episode> =
    map { it.toEpisode() }

@RestrictTo(RestrictTo.Scope.TESTS)
fun Episode.toEpisodeWithExtrasView(): EpisodeWithExtrasView =
    EpisodeWithExtrasView(
        episode = toEpisodeEntity(),
        likedAt = likedAt,
        playedAt = playedAt,
        position = position,
        isCompleted = isCompleted,
    )

@RestrictTo(RestrictTo.Scope.TESTS)
fun List<Episode>.toEpisodeWithExtrasViews(): List<EpisodeWithExtrasView> =
    map { it.toEpisodeWithExtrasView() }

fun Episode.toEpisodeEntity(): EpisodeEntity =
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
    )

fun List<Episode>.toEpisodeEntities(): List<EpisodeEntity> =
    map { it.toEpisodeEntity() }

fun Soundbite.toSoundbiteEntity(
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
        cachedAt = cachedAt,
    )

fun List<Soundbite>.toSoundbiteEntities(
    cachedAt: Instant = Clock.System.now(),
): List<SoundbiteEntity> =
    map {
        it.toSoundbiteEntity(
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