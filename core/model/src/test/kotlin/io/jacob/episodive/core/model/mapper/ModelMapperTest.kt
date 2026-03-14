package io.jacob.episodive.core.model.mapper

import io.jacob.episodive.core.model.Category
import io.jacob.episodive.core.model.EpisodeType
import io.jacob.episodive.core.model.Medium
import io.jacob.episodive.core.model.RecentFeed
import io.jacob.episodive.core.model.TrendingFeed
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

class ModelMapperTest {

    @Test
    fun `toInstant converts Long to Instant correctly`() {
        // Given
        val epochSeconds = 1758000000L

        // When
        val instant = epochSeconds.toInstant()

        // Then
        assertEquals(Instant.fromEpochSeconds(epochSeconds), instant)
    }

    @Test
    fun `toSeconds converts Instant to Long correctly`() {
        // Given
        val instant = Instant.fromEpochSeconds(1758000000L)

        // When
        val seconds = instant.toSeconds()

        // Then
        assertEquals(1758000000L, seconds)
    }

    @Test
    fun `toDurationSeconds converts Int to Duration correctly`() {
        // Given
        val seconds = 1234

        // When
        val duration = seconds.toDurationSeconds()

        // Then
        assertEquals(1234.seconds, duration)
    }

    @Test
    fun `toIntSeconds converts Duration to Int correctly`() {
        // Given
        val duration = 1234.seconds

        // When
        val intSeconds = duration.toIntSeconds()

        // Then
        assertEquals(1234, intSeconds)
    }

    @Test
    fun `toMedium converts valid String to Medium correctly`() {
        // Given & When & Then
        assertEquals(Medium.PODCAST, "podcast".toMedium())
        assertEquals(Medium.MUSIC, "music".toMedium())
        assertEquals(Medium.VIDEO, "video".toMedium())
        assertEquals(Medium.FILM, "film".toMedium())
        assertEquals(Medium.AUDIOBOOK, "audiobook".toMedium())
        assertEquals(Medium.NEWSLETTER, "newsletter".toMedium())
        assertEquals(Medium.BLOG, "blog".toMedium())
    }

    @Test
    fun `toMedium returns null for invalid String`() {
        // Given & When & Then
        assertNull("invalid".toMedium())
        assertNull("".toMedium())
        assertNull("PODCAST".toMedium()) // case sensitive
    }

    @Test
    fun `toValue converts Medium to String correctly`() {
        // Given & When & Then
        assertEquals("podcast", Medium.PODCAST.toValue())
        assertEquals("music", Medium.MUSIC.toValue())
        assertEquals("video", Medium.VIDEO.toValue())
        assertEquals("film", Medium.FILM.toValue())
        assertEquals("audiobook", Medium.AUDIOBOOK.toValue())
        assertEquals("newsletter", Medium.NEWSLETTER.toValue())
        assertEquals("blog", Medium.BLOG.toValue())
    }

    @Test
    fun `toCategories converts Map to List of Category correctly`() {
        // Given
        val categoriesMap = mapOf(
            1 to "Arts",
            9 to "Business",
            16 to "Comedy"
        )

        // When
        val categories = categoriesMap.toCategories()

        // Then
        assertEquals(3, categories.size)
        assertTrue(categories.contains(Category.ARTS))
        assertTrue(categories.contains(Category.BUSINESS))
        assertTrue(categories.contains(Category.COMEDY))
    }

    @Test
    fun `toCategories ignores invalid category IDs`() {
        // Given
        val categoriesMap = mapOf(
            1 to "Arts",
            999 to "Invalid", // This should be ignored
            9 to "Business"
        )

        // When
        val categories = categoriesMap.toCategories()

        // Then
        assertTrue(categories.contains(Category.ARTS))
        assertTrue(categories.contains(Category.BUSINESS))
        assertEquals(2, categories.size) // Invalid category ignored
    }

    @Test
    fun `toMap converts List of Category to Map correctly`() {
        // Given
        val categories = listOf(Category.ARTS, Category.BUSINESS, Category.COMEDY)

        // When
        val map = categories.toMap()

        // Then
        assertEquals(3, map.size)
        assertEquals("Arts", map[1])
        assertEquals("Business", map[9])
        assertEquals("Comedy", map[16])
    }

    @Test
    fun `toCommaString converts List of Category to comma-separated string correctly`() {
        // Given
        val categories = listOf(Category.BUSINESS, Category.ARTS, Category.COMEDY) // Unsorted

        // When
        val commaString = categories.toCommaString()

        // Then
        assertEquals("1,9,16", commaString) // Should be sorted by ID
    }

    @Test
    fun `toCommaString returns empty string for empty list`() {
        // Given
        val categories = emptyList<Category>()

        // When
        val commaString = categories.toCommaString()

        // Then
        assertEquals("", commaString)
    }

    @Test
    fun `String toCategories converts comma-separated string to List of Category correctly`() {
        // Given
        val categoriesString = "1,9,16"

        // When
        val categories = categoriesString.toCategories()

        // Then
        assertTrue(categories.contains(Category.ARTS))
        assertTrue(categories.contains(Category.BUSINESS))
        assertTrue(categories.contains(Category.COMEDY))
        assertEquals(3, categories.size)
    }

    @Test
    fun `String toCategories returns empty list for empty string`() {
        // Given
        val categoriesString = ""

        // When
        val categories = categoriesString.toCategories()

        // Then
        assertTrue(categories.isEmpty())
    }

    @Test
    fun `String toCategories ignores invalid category IDs`() {
        // Given
        val categoriesString = "1,999,9" // 999 is invalid

        // When
        val categories = categoriesString.toCategories()

        // Then
        assertTrue(categories.contains(Category.ARTS))
        assertTrue(categories.contains(Category.BUSINESS))
        assertEquals(2, categories.size) // Invalid category ignored
    }

    @Test
    fun `String toCategories handles malformed input gracefully`() {
        // Given
        val categoriesString = "1,abc,9,,"

        // When
        val categories = categoriesString.toCategories()

        // Then
        assertTrue(categories.contains(Category.ARTS))
        assertTrue(categories.contains(Category.BUSINESS))
        assertEquals(2, categories.size)
    }

    @Test
    fun `toEpisodeType converts valid String to EpisodeType correctly`() {
        // Given & When & Then
        assertEquals(EpisodeType.FULL, "full".toEpisodeType())
        assertEquals(EpisodeType.TRAILER, "trailer".toEpisodeType())
        assertEquals(EpisodeType.BONUS, "bonus".toEpisodeType())
    }

    @Test
    fun `toEpisodeType returns null for invalid String`() {
        // Given & When & Then
        assertNull("invalid".toEpisodeType())
        assertNull("".toEpisodeType())
        assertNull("FULL".toEpisodeType()) // case sensitive
    }

    @Test
    fun `EpisodeType toValue converts to String correctly`() {
        // Given & When & Then
        assertEquals("full", EpisodeType.FULL.toValue())
        assertEquals("trailer", EpisodeType.TRAILER.toValue())
        assertEquals("bonus", EpisodeType.BONUS.toValue())
    }

    @Test
    fun `toFeedFromRecent converts RecentFeed to Feed correctly`() {
        // Given
        val recentFeed = RecentFeed(
            id = 123L,
            url = "https://example.com/feed.xml",
            title = "Test Podcast",
            newestItemPublishTime = Instant.fromEpochSeconds(1758000000),
            oldestItemPublishTime = Instant.fromEpochSeconds(1757000000),
            description = "Test description",
            author = "Test Author",
            image = "https://example.com/image.jpg",
            itunesId = 456L,
            trendScore = 95,
            language = "en",
            categories = listOf(Category.NEWS, Category.EDUCATION)
        )

        // When
        val feed = recentFeed.toFeedFromRecent()

        // Then
        assertEquals(recentFeed.id, feed.id)
        assertEquals(recentFeed.url, feed.url)
        assertEquals(recentFeed.title, feed.title)
        assertEquals(recentFeed.description, feed.description)
        assertEquals(recentFeed.image, feed.image)
        assertEquals(recentFeed.newestItemPublishTime, feed.newestItemPublishTime)
        assertEquals(recentFeed.itunesId, feed.itunesId)
        assertEquals(recentFeed.language, feed.language)
        assertEquals(recentFeed.categories, feed.categories)
    }

    @Test
    fun `toFeedsFromRecent converts List of RecentFeed to List of Feed correctly`() {
        // Given
        val recentFeeds = listOf(
            RecentFeed(
                id = 123L,
                url = "https://example.com/feed1.xml",
                title = "Test Podcast 1",
                newestItemPublishTime = Instant.fromEpochSeconds(1758000000),
                oldestItemPublishTime = null,
                description = "Test description 1",
                author = "Test Author 1",
                image = "https://example.com/image1.jpg",
                itunesId = 456L,
                trendScore = 95,
                language = "en",
                categories = listOf(Category.NEWS)
            ),
            RecentFeed(
                id = 124L,
                url = "https://example.com/feed2.xml",
                title = "Test Podcast 2",
                newestItemPublishTime = Instant.fromEpochSeconds(1758000001),
                oldestItemPublishTime = null,
                description = "Test description 2",
                author = "Test Author 2",
                image = "https://example.com/image2.jpg",
                itunesId = 457L,
                trendScore = 96,
                language = "ko",
                categories = listOf(Category.EDUCATION)
            )
        )

        // When
        val feeds = recentFeeds.toFeedsFromRecent()

        // Then
        assertEquals(recentFeeds.size, feeds.size)
        assertEquals(recentFeeds.first().id, feeds.first().id)
        assertEquals(recentFeeds.last().id, feeds.last().id)
    }

    @Test
    fun `toFeedFromTrending converts TrendingFeed to Feed correctly`() {
        // Given
        val trendingFeed = TrendingFeed(
            id = 789L,
            url = "https://example.com/trending.xml",
            title = "Trending Podcast",
            description = "Trending description",
            author = "Trending Author",
            image = "https://example.com/trending-image.jpg",
            artwork = "https://example.com/trending-artwork.jpg",
            newestItemPublishTime = Instant.fromEpochSeconds(1758000000),
            itunesId = 999L,
            trendScore = 98,
            language = "en",
            categories = listOf(Category.COMEDY, Category.NEWS)
        )

        // When
        val feed = trendingFeed.toFeedFromTrending()

        // Then
        assertEquals(trendingFeed.id, feed.id)
        assertEquals(trendingFeed.url, feed.url)
        assertEquals(trendingFeed.title, feed.title)
        assertEquals(trendingFeed.description, feed.description)
        assertEquals(trendingFeed.artwork, feed.image) // Note: artwork -> image
        assertEquals(trendingFeed.newestItemPublishTime, feed.newestItemPublishTime)
        assertEquals(trendingFeed.itunesId, feed.itunesId)
        assertEquals(trendingFeed.language, feed.language)
        assertEquals(trendingFeed.categories, feed.categories)
    }

    @Test
    fun `toFeedsFromTrending converts List of TrendingFeed to List of Feed correctly`() {
        // Given
        val trendingFeeds = listOf(
            TrendingFeed(
                id = 789L,
                url = "https://example.com/trending1.xml",
                title = "Trending Podcast 1",
                description = "Trending description 1",
                author = "Trending Author 1",
                image = "https://example.com/trending-image1.jpg",
                artwork = "https://example.com/trending-artwork1.jpg",
                newestItemPublishTime = Instant.fromEpochSeconds(1758000000),
                itunesId = 999L,
                trendScore = 98,
                language = "en",
                categories = listOf(Category.COMEDY)
            ),
            TrendingFeed(
                id = 790L,
                url = "https://example.com/trending2.xml",
                title = "Trending Podcast 2",
                description = "Trending description 2",
                author = "Trending Author 2",
                image = "https://example.com/trending-image2.jpg",
                artwork = "https://example.com/trending-artwork2.jpg",
                newestItemPublishTime = Instant.fromEpochSeconds(1758000001),
                itunesId = 1000L,
                trendScore = 99,
                language = "fr",
                categories = listOf(Category.NEWS)
            )
        )

        // When
        val feeds = trendingFeeds.toFeedsFromTrending()

        // Then
        assertEquals(trendingFeeds.size, feeds.size)
        assertEquals(trendingFeeds.first().id, feeds.first().id)
        assertEquals(trendingFeeds.last().id, feeds.last().id)
    }

    @Test
    fun `round trip conversion for Duration and Int works correctly`() {
        // Given
        val originalInt = 1234

        // When
        val duration = originalInt.toDurationSeconds()
        val backToInt = duration.toIntSeconds()

        // Then
        assertEquals(originalInt, backToInt)
    }

    @Test
    fun `round trip conversion for Instant and Long works correctly`() {
        // Given
        val originalLong = 1758000000L

        // When
        val instant = originalLong.toInstant()
        val backToLong = instant.toSeconds()

        // Then
        assertEquals(originalLong, backToLong)
    }

    @Test
    fun `round trip conversion for Medium and String works correctly`() {
        // Given
        val originalMedium = Medium.PODCAST

        // When
        val stringValue = originalMedium.toValue()
        val backToMedium = stringValue.toMedium()

        // Then
        assertEquals(originalMedium, backToMedium)
    }

    @Test
    fun `round trip conversion for EpisodeType and String works correctly`() {
        // Given
        val originalEpisodeType = EpisodeType.FULL

        // When
        val stringValue = originalEpisodeType.toValue()
        val backToEpisodeType = stringValue.toEpisodeType()

        // Then
        assertEquals(originalEpisodeType, backToEpisodeType)
    }

    @Test
    fun `round trip conversion for Categories works correctly`() {
        // Given
        val originalCategories = listOf(Category.ARTS, Category.BUSINESS, Category.COMEDY)

        // When
        val map = originalCategories.toMap()
        val backToCategories = map.toCategories()

        // Then
        assertEquals(originalCategories.size, backToCategories.size)
        assertTrue(backToCategories.containsAll(originalCategories))
    }

    @Test
    fun `round trip conversion for Categories to comma string works correctly`() {
        // Given
        val originalCategories = listOf(Category.ARTS, Category.BUSINESS, Category.COMEDY)

        // When
        val commaString = originalCategories.toCommaString()
        val backToCategories = commaString.toCategories()

        // Then
        assertEquals(originalCategories.size, backToCategories.size)
        assertTrue(backToCategories.containsAll(originalCategories))
    }
}