package io.jacob.episodive.core.database.datasource

import io.jacob.episodive.core.database.dao.SoundbiteDao
import io.jacob.episodive.core.database.mapper.toSoundbiteEntities
import io.jacob.episodive.core.testing.model.soundbiteTestDataList
import io.jacob.episodive.core.testing.util.MainDispatcherRule
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.confirmVerified
import io.mockk.just
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Rule
import org.junit.Test

class SoundbiteLocalDataSourceTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val soundbiteDao = mockk<SoundbiteDao>(relaxed = true)

    private val dataSource: SoundbiteLocalDataSource = SoundbiteLocalDataSourceImpl(
        soundbiteDao = soundbiteDao,
    )

    private val cacheKey = "test_cache"
    private val soundbiteEntities = soundbiteTestDataList.toSoundbiteEntities()

    @After
    fun teardown() {
        confirmVerified(soundbiteDao)
    }

    @Test
    fun `Given dependencies, When upsertSoundbites, Then call dao's method`() =
        runTest {
            // Given
            coEvery { soundbiteDao.upsertSoundbites(any()) } just Runs

            // When
            dataSource.upsertSoundbites(soundbiteEntities)

            // Then
            coVerify { soundbiteDao.upsertSoundbites(soundbiteEntities) }
        }

    @Test
    fun `Given dependencies, When deleteSoundbites, Then call dao's method`() =
        runTest {
            // Given
            coEvery { soundbiteDao.deleteSoundbites() } just Runs

            // When
            dataSource.deleteSoundbites()

            // Then
            coVerify { soundbiteDao.deleteSoundbites() }
        }

    @Test
    fun `Given dependencies, When replaceSoundbites, Then call dao's method`() =
        runTest {
            // Given
            coEvery { soundbiteDao.replaceSoundbites(any()) } just Runs

            // When
            dataSource.replaceSoundbites(soundbiteEntities)

            // Then
            coVerify { soundbiteDao.replaceSoundbites(soundbiteEntities) }
        }

    @Test
    fun `Given dependencies, When getSoundbites, Then call dao's method`() =
        runTest {
            // Given
            coEvery { soundbiteDao.getSoundbites(10) } returns mockk()

            // When
            dataSource.getSoundbites(10)

            // Then
            coVerify { soundbiteDao.getSoundbites(10) }
        }

    @Test
    fun `Given dependencies, When getSoundbitesPaging, Then call dao's method`() =
        runTest {
            // Given
            coEvery { soundbiteDao.getSoundbitesPaging() } returns mockk()

            // When
            dataSource.getSoundbitesPaging()

            // Then
            coVerify { soundbiteDao.getSoundbitesPaging() }
        }

    @Test
    fun `Given dependencies, When getSoundbitesOldestCachedAt, Then call dao's method`() =
        runTest {
            // Given
            coEvery { soundbiteDao.getSoundbitesOldestCachedAt() } returns mockk()

            // When
            dataSource.getSoundbitesOldestCachedAt()

            // Then
            coVerify { soundbiteDao.getSoundbitesOldestCachedAt() }
        }

    @Test
    fun `Given offset and limit, When getSoundbitesPagingList, Then calls dao getSoundbitesPagingList`() = runTest {
        // Given
        coEvery { soundbiteDao.getSoundbitesPagingList(any(), any()) } returns emptyList()

        // When
        dataSource.getSoundbitesPagingList(offset = 0, limit = 10)

        // Then
        coVerify { soundbiteDao.getSoundbitesPagingList(0, 10) }
    }
}