package io.jacob.episodive.core.datastore.mapper

import io.jacob.episodive.core.datastore.model.UserPreferences
import io.jacob.episodive.core.model.Category
import io.jacob.episodive.core.model.UserData
import io.jacob.episodive.core.testing.util.MainDispatcherRule
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class DataStoreMapperTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `toUserData converts UserPreferences to UserData correctly`() {
        // Given
        val userPreferences = UserPreferences(
            isFirstLaunch = false,
            language = "en-US",
            categories = listOf(Category.ARTS, Category.BUSINESS, Category.COMEDY),
            speed = 1f,
        )

        // When
        val userData = userPreferences.toUserData()

        // Then
        assertEquals(userPreferences.isFirstLaunch, userData.isFirstLaunch)
        assertEquals(userPreferences.language, userData.language)
        assertEquals(userPreferences.categories, userData.categories)
        assertEquals(3, userData.categories.size)
        assertEquals(Category.ARTS, userData.categories[0])
        assertEquals(Category.BUSINESS, userData.categories[1])
        assertEquals(Category.COMEDY, userData.categories[2])
        assertEquals(1f, userData.speed)
    }

    @Test
    fun `toUserPreferences converts UserData to UserPreferences correctly`() {
        // Given
        val userData = UserData(
            isFirstLaunch = true,
            language = "ko-KR",
            categories = listOf(Category.NEWS, Category.EDUCATION, Category.TECHNOLOGY),
            speed = 1f,
        )

        // When
        val userPreferences = userData.toUserPreferences()

        // Then
        assertEquals(userData.isFirstLaunch, userPreferences.isFirstLaunch)
        assertEquals(userData.language, userPreferences.language)
        assertEquals(userData.categories, userPreferences.categories)
        assertEquals(3, userPreferences.categories.size)
        assertEquals(Category.NEWS, userPreferences.categories[0])
        assertEquals(Category.EDUCATION, userPreferences.categories[1])
        assertEquals(Category.TECHNOLOGY, userPreferences.categories[2])
        assertEquals(1f, userData.speed)
    }

    @Test
    fun `toUserData handles empty categories list correctly`() {
        // Given
        val userPreferences = UserPreferences(
            isFirstLaunch = true,
            language = "fr-FR",
            categories = emptyList(),
            speed = 1f,
        )

        // When
        val userData = userPreferences.toUserData()

        // Then
        assertEquals(userPreferences.isFirstLaunch, userData.isFirstLaunch)
        assertEquals(userPreferences.language, userData.language)
        assertEquals(userPreferences.categories, userData.categories)
        assertEquals(0, userData.categories.size)
        assertEquals(1f, userData.speed)
    }

    @Test
    fun `toUserPreferences handles empty categories list correctly`() {
        // Given
        val userData = UserData(
            isFirstLaunch = false,
            language = "de-DE",
            categories = emptyList(),
            speed = 1f,
        )

        // When
        val userPreferences = userData.toUserPreferences()

        // Then
        assertEquals(userData.isFirstLaunch, userPreferences.isFirstLaunch)
        assertEquals(userData.language, userPreferences.language)
        assertEquals(userData.categories, userPreferences.categories)
        assertEquals(0, userPreferences.categories.size)
        assertEquals(1f, userData.speed)
    }

    @Test
    fun `round trip conversion works correctly`() {
        // Given
        val originalUserData = UserData(
            isFirstLaunch = true,
            language = "ja-JP",
            categories = listOf(
                Category.ARTS,
                Category.BUSINESS,
                Category.COMEDY,
                Category.EDUCATION,
                Category.GAMES,
                Category.HEALTH,
                Category.FITNESS,
                Category.HISTORY,
                Category.KIDS,
                Category.FAMILY,
                Category.LEISURE,
                Category.MUSIC,
                Category.NEWS,
                Category.RELIGION,
                Category.SPIRITUALITY,
                Category.SCIENCE,
                Category.SOCIETY,
                Category.CULTURE,
                Category.SPORTS,
                Category.TECHNOLOGY,
                Category.TRUE_CRIME,
                Category.TV,
                Category.FILM
            ),
            speed = 1f,
        )

        // When
        val userPreferences = originalUserData.toUserPreferences()
        val backToUserData = userPreferences.toUserData()

        // Then
        assertEquals(originalUserData.isFirstLaunch, backToUserData.isFirstLaunch)
        assertEquals(originalUserData.language, backToUserData.language)
        assertEquals(originalUserData.categories, backToUserData.categories)
        assertEquals(originalUserData.categories.size, backToUserData.categories.size)

        // Verify all categories are preserved
        originalUserData.categories.forEachIndexed { index, category ->
            assertEquals(category, backToUserData.categories[index])
        }
        assertEquals(1f, backToUserData.speed)
    }

    @Test
    fun `round trip conversion with UserPreferences as starting point works correctly`() {
        // Given
        val originalUserPreferences = UserPreferences(
            isFirstLaunch = false,
            language = "es-ES",
            categories = listOf(
                Category.POLITICS,
                Category.DOCUMENTARY,
                Category.PERSONAL,
                Category.JOURNALS,
                Category.PHILOSOPHY,
                Category.SELF_IMPROVEMENT,
                Category.MENTAL,
                Category.NUTRITION,
                Category.SEXUALITY,
                Category.RELATIONSHIPS,
                Category.PARENTING,
                Category.PETS,
                Category.ANIMALS,
                Category.PLACES,
                Category.TRAVEL,
                Category.FICTION,
                Category.DRAMA,
                Category.NATURE,
                Category.PHYSICS,
                Category.CHEMISTRY,
                Category.MATHEMATICS,
                Category.SOCIAL
            ),
            speed = 1f,
        )

        // When
        val userData = originalUserPreferences.toUserData()
        val backToUserPreferences = userData.toUserPreferences()

        // Then
        assertEquals(originalUserPreferences.isFirstLaunch, backToUserPreferences.isFirstLaunch)
        assertEquals(originalUserPreferences.language, backToUserPreferences.language)
        assertEquals(originalUserPreferences.categories, backToUserPreferences.categories)
        assertEquals(originalUserPreferences.categories.size, backToUserPreferences.categories.size)

        // Verify all categories are preserved
        originalUserPreferences.categories.forEachIndexed { index, category ->
            assertEquals(category, backToUserPreferences.categories[index])
        }
        assertEquals(1f, backToUserPreferences.speed)
    }

    @Test
    fun `conversion preserves language codes correctly`() {
        // Test various language codes
        val languageCodes = listOf(
            "en",
            "en-US",
            "en-GB",
            "ko",
            "ko-KR",
            "ja",
            "ja-JP",
            "zh",
            "zh-CN",
            "zh-TW",
            "fr",
            "fr-FR",
            "de",
            "de-DE",
            "es",
            "es-ES",
            "it",
            "it-IT",
            "pt",
            "pt-BR",
            "ru",
            "ru-RU"
        )

        languageCodes.forEach { languageCode ->
            // Given
            val userData = UserData(
                isFirstLaunch = true,
                language = languageCode,
                categories = listOf(Category.NEWS),
                speed = 1f
            )

            // When
            val userPreferences = userData.toUserPreferences()
            val backToUserData = userPreferences.toUserData()

            // Then
            assertEquals("Language code $languageCode should be preserved", languageCode, backToUserData.language)
            assertEquals(1f, backToUserData.speed)
        }
    }

    @Test
    fun `conversion preserves single category correctly`() {
        // Test each category individually to ensure all are handled correctly
        Category.entries.forEach { category ->
            // Given
            val userData = UserData(
                isFirstLaunch = false,
                language = "en-US",
                categories = listOf(category),
                speed = 1f
            )

            // When
            val userPreferences = userData.toUserPreferences()
            val backToUserData = userPreferences.toUserData()

            // Then
            assertEquals("Category $category should be preserved", category, backToUserData.categories.single())
            assertEquals(1f, backToUserData.speed)
        }
    }
}