package io.jacob.episodive.core.database.util

import io.jacob.episodive.core.model.Soundbite
import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

class SoundbiteConverterTest {
    private val converter = SoundbiteConverter()

    @Test
    fun `Given soundbite list, when converting to and from string, then returns same list`() {
        // Given
        val soundbites = listOf(
            Soundbite(
                enclosureUrl = "https://example.com/episode1.mp3",
                title = "Intro",
                startTime = Instant.fromEpochSeconds(10),
                duration = 30.seconds,
                episodeId = 1L,
                episodeTitle = "Episode 1",
                feedTitle = "Test Feed",
                feedUrl = "https://example.com/feed",
                feedId = 100L,
            ),
            Soundbite(
                enclosureUrl = "https://example.com/episode2.mp3",
                title = "Main Topic",
                startTime = Instant.fromEpochSeconds(100),
                duration = 45.seconds,
                episodeId = 2L,
                episodeTitle = "Episode 2",
                feedTitle = "Test Feed",
                feedUrl = "https://example.com/feed",
                feedId = 100L,
            ),
        )

        // When
        val json = converter.fromSoundbiteList(soundbites)
        val result = converter.toSoundbiteList(json)

        // Then
        assertEquals(soundbites, result)
    }

    @Test
    fun `Given empty list, when converting, then returns empty list`() {
        // Given
        val soundbites = emptyList<Soundbite>()

        // When
        val json = converter.fromSoundbiteList(soundbites)
        val result = converter.toSoundbiteList(json)

        // Then
        assertEquals(emptyList<Soundbite>(), result)
    }
}