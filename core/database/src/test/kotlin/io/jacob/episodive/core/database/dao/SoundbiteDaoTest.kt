package io.jacob.episodive.core.database.dao

import app.cash.turbine.test
import io.jacob.episodive.core.database.RoomDatabaseRule
import io.jacob.episodive.core.database.mapper.toSoundbiteEntities
import io.jacob.episodive.core.testing.model.soundbiteTestDataList
import io.jacob.episodive.core.testing.util.MainDispatcherRule
import io.jacob.episodive.core.testing.util.loadAsSnapshot
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SoundbiteDaoTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @get:Rule
    val dbRule = RoomDatabaseRule()

    private lateinit var dao: SoundbiteDao

    @Before
    fun setup() {
        dao = dbRule.db.soundbiteDao()
    }

    private val soundbiteEntities = soundbiteTestDataList.toSoundbiteEntities()

    @Test
    fun `Given soundbites, When upsertSoundbites, Then upserted correctly`() =
        runTest {
            // Given
            dao.upsertSoundbites(soundbiteEntities)

            // When
            dao.getSoundbites(10).test {
                val items = awaitItem()
                // Then
                assertEquals(items.size, soundbiteEntities.size)
                cancel()
            }

            // When
            dao.deleteSoundbite(soundbiteEntities.first().episodeId)
            dao.getSoundbites(10).test {
                val items = awaitItem()
                // Then
                assertEquals(items.size, soundbiteEntities.size - 1)
                cancel()
            }

            // When
            dao.deleteSoundbites()
            dao.getSoundbites(10).test {
                val items = awaitItem()
                // Then
                assertEquals(items.size, 0)
                cancel()
            }
        }

    @Test
    fun `Given some soundbites, When deleteSoundbites, Then deleted correctly`() =
        runTest {
            // Given
            dao.upsertSoundbites(soundbiteEntities)

            // When
            dao.deleteSoundbites()
            dao.getSoundbites(10).test {
                val items = awaitItem()
                // Then
                assertEquals(items.size, 0)
                cancel()
            }
        }

    @Test
    fun `Given soundbites with different cache keys, When replaceSoundbites, Then replaced by cache key`() =
        runTest {
            // Given - Insert initial soundbites
            val initialSoundbites = soundbiteEntities.take(3)
            dao.upsertSoundbites(initialSoundbites)

            // When - Replace with new soundbites
            val newSoundbites = listOf(
                soundbiteEntities[5].copy(episodeId = 400L),
                soundbiteEntities[6].copy(episodeId = 401L)
            )
            dao.replaceSoundbites(newSoundbites)

            // Then
            dao.getSoundbites(10).test {
                val items = awaitItem()
                assertEquals(2, items.size)
                assertTrue(items.any { it.episodeId == 400L })
                assertTrue(items.any { it.episodeId == 401L })
                cancel()
            }
        }

    @Test
    fun `Given soundbites, When getSoundbitesPaging is called, Then soundbites are returned`() =
        runTest {
            // Given
            dao.upsertSoundbites(soundbiteEntities)

            // When
            val soundbites = dao.getSoundbitesPaging().loadAsSnapshot()

            // Then
            assertEquals(10, soundbites.size)
        }

    @Test
    fun `Given multiple soundbites with same cache key, When getSoundbitesOldestCachedAt is called, Then oldest cachedAt is returned`() =
        runTest {
            // Given
            dao.upsertSoundbites(soundbiteEntities)

            // When
            val oldestCachedAt = dao.getSoundbitesOldestCachedAt()

            // Then
            val expectedOldest = soundbiteEntities.minByOrNull { it.cachedAt }?.cachedAt
            assertEquals(expectedOldest?.epochSeconds, oldestCachedAt?.epochSeconds)
        }
}