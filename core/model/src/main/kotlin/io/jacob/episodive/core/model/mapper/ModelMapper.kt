package io.jacob.episodive.core.model.mapper

import io.jacob.episodive.core.model.Category
import io.jacob.episodive.core.model.EpisodeType
import io.jacob.episodive.core.model.Feed
import io.jacob.episodive.core.model.Medium
import io.jacob.episodive.core.model.RecentFeed
import io.jacob.episodive.core.model.TrendingFeed
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.number
import kotlinx.datetime.toLocalDateTime
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

fun Long.toInstant(): Instant = Instant.fromEpochSeconds(this)
fun Instant.toSeconds(): Long = epochSeconds
fun Instant.toHumanReadable(): String {
    val localDateTime = this.toLocalDateTime(TimeZone.currentSystemDefault())

    // kotlinx-datetime → java.time 변환
    val javaLocalDateTime = LocalDateTime.of(
        localDateTime.year,
        localDateTime.month.number,
        localDateTime.day,
        localDateTime.hour,
        localDateTime.minute,
        localDateTime.second
    )

    val outputFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG)
        .withLocale(Locale.getDefault())

    return javaLocalDateTime.format(outputFormatter)
}

fun Instant.toRelativeDate(): String {
    val locale = Locale.getDefault()
    val timeZone = TimeZone.currentSystemDefault()
    val now = Clock.System.now()

    // Convert to LocalDate for comparison
    val thisDate = this.toLocalDateTime(timeZone).date
    val todayDate = now.toLocalDateTime(timeZone).date
    val yesterdayDate = now.minus(1, DateTimeUnit.DAY, timeZone).toLocalDateTime(timeZone).date
    val dayBeforeYesterdayDate =
        now.minus(2, DateTimeUnit.DAY, timeZone).toLocalDateTime(timeZone).date

    val thisYear = now.toLocalDateTime(timeZone).year

    return when {
        thisDate == todayDate -> {
            if (locale.language == "ko") "오늘" else "Today"
        }

        thisDate == yesterdayDate -> {
            if (locale.language == "ko") "어제" else "Yesterday"
        }

        thisDate == dayBeforeYesterdayDate -> {
            if (locale.language == "ko") "그저께" else "2 days ago"
        }

        thisDate.year == thisYear -> {
            // This year: show month and day only
            val localDateTime = this.toLocalDateTime(timeZone)
            val javaLocalDateTime = LocalDateTime.of(
                localDateTime.year,
                localDateTime.month.number,
                localDateTime.day,
                0, 0, 0
            )

            if (locale.language == "ko") {
                val formatter = DateTimeFormatter.ofPattern("M월 d일")
                javaLocalDateTime.format(formatter)
            } else {
                val formatter = DateTimeFormatter.ofPattern("MMM d")
                javaLocalDateTime.format(formatter)
            }
        }

        else -> {
            // Different year: show year, month, and day
            val localDateTime = this.toLocalDateTime(timeZone)
            val javaLocalDateTime = LocalDateTime.of(
                localDateTime.year,
                localDateTime.month.number,
                localDateTime.day,
                0, 0, 0
            )

            if (locale.language == "ko") {
                val formatter = DateTimeFormatter.ofPattern("yyyy년 M월 d일")
                javaLocalDateTime.format(formatter)
            } else {
                val formatter = DateTimeFormatter.ofPattern("MMM d, yyyy")
                javaLocalDateTime.format(formatter)
            }
        }
    }
}

fun Int.toDurationSeconds(): Duration = seconds
fun Long.toDurationMillis(): Duration = milliseconds
fun Duration.toIntSeconds(): Int = inWholeSeconds.toInt()
fun Duration.toLongMillis(): Long = inWholeMilliseconds
fun Duration.toHumanReadable(): String {
    val locale = Locale.getDefault()

    return toComponents { hours, minutes, seconds, _ ->
        when (locale.language) {
            "ko" -> buildString {
                if (hours > 0) append("${hours}시간 ")
                if (minutes > 0) append("${minutes}분 ")
                if (seconds > 0 || isEmpty()) append("${seconds}초")
            }.trim()

            else -> buildString {
                if (hours > 0) append("${hours}hr ")
                if (minutes > 0) append("${minutes}min ")
                if (seconds > 0 || isEmpty()) append("${seconds}sec")
            }.trim()
        }
    }
}

fun Duration.toMediaTime(): String {
    return toComponents { hours, minutes, seconds, _ ->
        if (hours > 0) {
            "$hours:%02d:%02d".format(minutes, seconds)
        } else {
            "$minutes:%02d".format(seconds)
        }
    }
}

fun String.toMedium(): Medium? = Medium.entries.find { it.value == this }
fun Medium.toValue(): String = value

fun Map<Int, String>.toCategories(): List<Category> =
    this.mapNotNull { (id, _) ->
        Category.entries.find { it.id == id }
    }.toList()

fun List<Category>.toMap(): Map<Int, String> =
    associateBy({ it.id }, { it.label })

fun List<Category>.toCommaString(): String =
    sortedBy { it.id }.joinToString(",") { it.id.toString() }

fun List<Category>.toLabels(): String =
    sortedBy { it.id }.joinToString(", ") { it.label }

fun String.toCategories(): List<Category> =
    if (this.isEmpty()) {
        emptyList()
    } else {
        this.split(",").mapNotNull { id ->
            Category.entries.find { it.id == id.toIntOrNull() }
        }
    }

fun String.toEpisodeType(): EpisodeType? = EpisodeType.entries.find { it.value == this }
fun EpisodeType.toValue(): String = value

fun RecentFeed.toFeedFromRecent(): Feed =
    Feed(
        id = id,
        url = url,
        title = title,
        newestItemPublishTime = newestItemPublishTime,
        description = description,
        author = author,
        image = image,
        itunesId = itunesId,
        trendScore = trendScore,
        language = language,
        categories = categories,
    )

fun List<RecentFeed>.toFeedsFromRecent(): List<Feed> = map { it.toFeedFromRecent() }

fun TrendingFeed.toFeedFromTrending(): Feed =
    Feed(
        id = id,
        url = url,
        title = title,
        newestItemPublishTime = newestItemPublishTime,
        description = description,
        author = author,
        image = artwork,
        itunesId = itunesId,
        trendScore = trendScore,
        language = language,
        categories = categories,
    )

fun List<TrendingFeed>.toFeedsFromTrending(): List<Feed> = map { it.toFeedFromTrending() }