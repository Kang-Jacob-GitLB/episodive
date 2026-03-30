package io.jacob.episodive.core.database.dao

import app.cash.turbine.test
import io.jacob.episodive.core.database.RoomDatabaseRule
import io.jacob.episodive.core.database.model.RecentSearchEntity
import io.jacob.episodive.core.model.RecentSearchType
import io.jacob.episodive.core.testing.util.MainDispatcherRule
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.time.Clock
import kotlin.time.Duration.Companion.minutes

@RunWith(RobolectricTestRunner::class)
class RecentSearchDaoTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @get:Rule
    val dbRule = RoomDatabaseRule()

    private lateinit var dao: RecentSearchDao

    @Before
    fun setup() {
        dao = dbRule.db.recentSearchDao()
    }

    @Test
    fun `Given some recentSearchEntities, When getRecentSearches, Then return recentSearches`() =
        runTest {
            // Given
            val now = Clock.System.now()
            dao.upsertRecentSearch(
                RecentSearchEntity(
                    type = RecentSearchType.QUERY,
                    query = "query1",
                    searchedAt = now
                )
            )
            dao.upsertRecentSearch(
                RecentSearchEntity(
                    type = RecentSearchType.QUERY,
                    query = "query2",
                    searchedAt = now.plus(1.minutes)
                )
            )
            dao.upsertRecentSearch(
                RecentSearchEntity(
                    type = RecentSearchType.QUERY,
                    query = "query3",
                    searchedAt = now.plus(2.minutes)
                )
            )

            // When
            dao.getRecentSearches(3).test {
                val result = awaitItem()
                // Then
                assertEquals(3, result.size)
                assertEquals("query3", result[0].query)
                assertEquals("query2", result[1].query)
                assertEquals("query1", result[2].query)
                cancel()
            }
        }

    @Test
    fun `Given some recentSearchEntities, When getRecentSearches with limit, Then return limited recentSearches`() =
        runTest {
            // Given
            val now = Clock.System.now()
            dao.upsertRecentSearch(
                RecentSearchEntity(
                    type = RecentSearchType.QUERY,
                    query = "query1",
                    searchedAt = now
                )
            )
            dao.upsertRecentSearch(
                RecentSearchEntity(
                    type = RecentSearchType.QUERY,
                    query = "query2",
                    searchedAt = now.plus(1.minutes)
                )
            )
            dao.upsertRecentSearch(
                RecentSearchEntity(
                    type = RecentSearchType.QUERY,
                    query = "query3",
                    searchedAt = now.plus(2.minutes)
                )
            )

            // When
            dao.getRecentSearches(2).test {
                val result = awaitItem()
                // Then
                assertEquals(2, result.size)
                assertEquals("query3", result[0].query)
                assertEquals("query2", result[1].query)
                cancel()
            }
        }

    @Test
    fun `Given some recentSearchEntities, When deleteRecentSearch, Then remove recentSearch`() =
        runTest {
            // Given
            val now = Clock.System.now()
            dao.upsertRecentSearch(
                RecentSearchEntity(
                    type = RecentSearchType.QUERY,
                    query = "query1",
                    searchedAt = now
                )
            )
            dao.upsertRecentSearch(
                RecentSearchEntity(
                    type = RecentSearchType.QUERY,
                    query = "query2",
                    searchedAt = now.plus(1.minutes)
                )
            )
            dao.upsertRecentSearch(
                RecentSearchEntity(
                    type = RecentSearchType.QUERY,
                    query = "query3",
                    searchedAt = now.plus(2.minutes)
                )
            )

            // When — get the id of query2 and delete by id
            dao.getRecentSearches(3).test {
                val result = awaitItem()
                val query2Entity = result.first { it.query == "query2" }
                dao.deleteRecentSearch(query2Entity.id)
                val updatedResult = awaitItem()
                // Then
                assertEquals(2, updatedResult.size)
                assertEquals("query3", updatedResult[0].query)
                assertEquals("query1", updatedResult[1].query)
                cancel()
            }
        }
}
