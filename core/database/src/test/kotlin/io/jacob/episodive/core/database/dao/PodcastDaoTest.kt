package io.jacob.episodive.core.database.dao

import app.cash.turbine.test
import io.jacob.episodive.core.database.RoomDatabaseRule
import io.jacob.episodive.core.database.mapper.toPodcastEntities
import io.jacob.episodive.core.database.mapper.toPodcastEntity
import io.jacob.episodive.core.database.model.FollowedPodcastEntity
import io.jacob.episodive.core.database.model.PodcastGroupEntity
import io.jacob.episodive.core.testing.model.podcastTestData
import io.jacob.episodive.core.testing.model.podcastTestDataList
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
import kotlin.time.Instant

@RunWith(RobolectricTestRunner::class)
class PodcastDaoTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @get:Rule
    val dbRule = RoomDatabaseRule()

    private lateinit var dao: PodcastDao

    @Before
    fun setup() {
        dao = dbRule.db.podcastDao()
    }

    private val podcastEntity = podcastTestData.toPodcastEntity()
    private val podcastEntities = podcastTestDataList.toPodcastEntities()

    @Test
    fun `Given single podcast, When upsertPodcast is called, Then podcast is inserted`() =
        runTest {
            // Given
            dao.upsertPodcast(podcastEntity.copy(id = 100L, title = "aaa"))

            // When
            dao.getPodcasts(limit = 10).test {
                val podcasts = awaitItem()

                // Then
                assertEquals(1, podcasts.size)
                assertEquals(100L, podcasts[0].podcast.id)
                assertEquals("aaa", podcasts[0].podcast.title)

                cancel()
            }
        }

    @Test
    fun `Given single podcast, When upsertPodcast is called, Then podcast is updated`() =
        runTest {
            // Given
            dao.upsertPodcast(podcastEntity.copy(id = 100L, title = "aaa"))
            dao.upsertPodcast(podcastEntity.copy(id = 100L, title = "bbb"))

            // When
            dao.getPodcasts(limit = 10).test {
                val podcasts = awaitItem()

                // Then
                assertEquals(1, podcasts.size)
                assertEquals(100L, podcasts[0].podcast.id)
                assertEquals("bbb", podcasts[0].podcast.title)

                cancel()
            }
        }

    @Test
    fun `Given multiple podcasts, When upsertPodcasts is called, Then podcasts are upserted`() =
        runTest {
            // Given
            dao.upsertPodcasts(podcastEntities)
            dao.upsertPodcasts(podcastEntities.toMutableList().apply {
                removeFirst()
                add(podcastEntity.copy(id = 100L, lastUpdateTime = Instant.fromEpochSeconds(1)))
            })

            // When
            dao.getPodcasts(limit = 100).test {
                val podcasts = awaitItem()

                // Then
                assertEquals(11, podcasts.size)
                assertEquals(100L, podcasts.last().podcast.id)

                cancel()
            }
        }

    @Test
    fun `Given single podcastGroup, When upsertPodcastGroup is called, Then podcastGroup is upserted`() =
        runTest {
            dao.upsertPodcast(podcastEntities[0])
            // Given
            val group = PodcastGroupEntity(
                groupKey = "test_group",
                id = podcastEntities[0].id,
                order = 0,
                createdAt = Instant.fromEpochSeconds(0),
            )
            dao.upsertPodcastGroup(group)
            dao.upsertPodcastGroup(group.copy(order = 1))


            // When
            val groups = dao.getPodcastGroupsByGroupKey("test_group")

            // Then
            assertEquals(1, groups.size)
            assertEquals(1, groups[0].order)
        }

    @Test
    fun `Given multiple podcastGroups, When upsertPodcastGroups is called, Then podcastGroups are upserted`() =
        runTest {
            // Given
            dao.upsertPodcasts(podcastEntities.take(3))
            val groups = podcastEntities.take(3).mapIndexed { index, podcast ->
                PodcastGroupEntity(
                    groupKey = "test_group",
                    id = podcast.id,
                    order = index,
                    createdAt = Instant.fromEpochSeconds(0),
                )
            }

            // When
            dao.upsertPodcastGroups(groups)

            // Then
            val result = dao.getPodcastGroupsByGroupKey("test_group")
            assertEquals(3, result.size)
            assertEquals(listOf(0, 1, 2), result.map { it.order })
        }

    @Test
    fun `Given podcasts and groupKey, When upsertPodcastsWithGroup is called, Then podcasts and groups are upserted`() =
        runTest {
            // Given
            val podcasts = podcastEntities.take(3)

            // When
            dao.upsertPodcastsWithGroup(podcasts, "test_group")

            // Then
            dao.getPodcastsByGroupKey("test_group", 10).test {
                val result = awaitItem()
                assertEquals(3, result.size)
                assertEquals(podcasts.map { it.id }, result.map { it.podcast.id })
                cancel()
            }
        }

    @Test
    fun `Given podcast id, When deletePodcast is called, Then podcast is deleted`() =
        runTest {
            // Given
            dao.upsertPodcast(podcastEntity.copy(id = 100L))

            // When
            dao.deletePodcast(100L)

            // Then
            dao.getPodcasts(limit = 10).test {
                val podcasts = awaitItem()
                assertEquals(0, podcasts.size)
                cancel()
            }
        }

    @Test
    fun `Given multiple podcasts, When deletePodcasts is called, Then all podcasts are deleted`() =
        runTest {
            // Given
            dao.upsertPodcasts(podcastEntities)

            // When
            dao.deletePodcasts()

            // Then
            dao.getPodcasts(limit = 100).test {
                val podcasts = awaitItem()
                assertEquals(0, podcasts.size)
                cancel()
            }
        }

    @Test
    fun `Given orphaned podcast, When deletePodcastsIfOrphaned is called, Then podcast is deleted`() =
        runTest {
            // Given
            dao.upsertPodcasts(podcastEntities.take(2))

            // When
            dao.deletePodcastsIfOrphaned(podcastEntities.take(2).map { it.id })

            // Then
            dao.getPodcasts(limit = 10).test {
                val podcasts = awaitItem()
                assertEquals(0, podcasts.size)
                cancel()
            }
        }

    @Test
    fun `Given followed podcast, When deletePodcastsIfOrphaned is called, Then podcast is not deleted`() =
        runTest {
            // Given
            dao.upsertPodcast(podcastEntity.copy(id = 100L))
            dao.addFollowedPodcast(
                FollowedPodcastEntity(
                    id = 100L,
                    followedAt = Instant.fromEpochSeconds(0),
                    isNotificationEnabled = false,
                )
            )

            // When
            dao.deletePodcastsIfOrphaned(listOf(100L))

            // Then
            dao.getPodcasts(limit = 10).test {
                val podcasts = awaitItem()
                assertEquals(1, podcasts.size)
                assertEquals(100L, podcasts[0].podcast.id)
                cancel()
            }
        }

    @Test
    fun `Given podcast in group, When deletePodcastsIfOrphaned is called, Then podcast is not deleted`() =
        runTest {
            // Given
            dao.upsertPodcastsWithGroup(listOf(podcastEntity.copy(id = 100L)), "test_group")

            // When
            dao.deletePodcastsIfOrphaned(listOf(100L))

            // Then
            dao.getPodcasts(limit = 10).test {
                val podcasts = awaitItem()
                assertEquals(1, podcasts.size)
                assertEquals(100L, podcasts[0].podcast.id)
                cancel()
            }
        }

    @Test
    fun `Given groupKey, When deletePodcastGroupsByGroupKey is called, Then groups are deleted`() =
        runTest {
            // Given
            dao.upsertPodcastsWithGroup(podcastEntities.take(3), "test_group")

            // When
            dao.deletePodcastGroupsByGroupKey("test_group")

            // Then
            val groups = dao.getPodcastGroupsByGroupKey("test_group")
            assertEquals(0, groups.size)
        }

    @Test
    fun `Given existing podcast id, When getPodcastById is called, Then podcast is returned`() =
        runTest {
            // Given
            dao.upsertPodcast(podcastEntity.copy(id = 100L))

            // When
            dao.getPodcastById(100L).test {
                val podcast = awaitItem()
                // Then
                assertEquals(100L, podcast?.podcast?.id)
                cancel()
            }
        }

    @Test
    fun `Given non-existing podcast id, When getPodcastById is called, Then null is returned`() =
        runTest {
            // When
            dao.getPodcastById(999L).test {
                val podcast = awaitItem()
                // Then
                assertEquals(null, podcast)
                cancel()
            }
        }

    @Test
    fun `Given podcast ids, When getPodcastsByIds is called, Then podcasts are returned`() =
        runTest {
            // Given
            dao.upsertPodcasts(podcastEntities.take(3))
            val ids = podcastEntities.take(3).map { it.id }

            // When
            dao.getPodcastsByIds(ids).test {
                val podcasts = awaitItem()
                // Then
                assertEquals(3, podcasts.size)
                assertTrue(podcasts.map { it.podcast.id }.contains(ids[0]))
                assertTrue(podcasts.map { it.podcast.id }.contains(ids[1]))
                assertTrue(podcasts.map { it.podcast.id }.contains(ids[2]))
                cancel()
            }
        }

    @Test
    fun `Given no query, When getPodcasts is called, Then all podcasts are returned`() =
        runTest {
            // Given
            dao.upsertPodcasts(podcastEntities)

            // When
            dao.getPodcasts(limit = 100).test {
                val podcasts = awaitItem()
                // Then
                assertEquals(10, podcasts.size)
                cancel()
            }
        }

    @Test
    fun `Given query, When getPodcasts is called, Then filtered podcasts are returned`() =
        runTest {
            // Given
            dao.upsertPodcasts(
                listOf(
                    podcastEntity.copy(id = 100L, title = "Java Programming"),
                    podcastEntity.copy(id = 101L, title = "Kotlin Programming"),
                )
            )

            // When
            dao.getPodcasts(query = "Kotlin", limit = 10).test {
                val podcasts = awaitItem()
                // Then
                assertEquals(1, podcasts.size)
                assertEquals("Kotlin Programming", podcasts[0].podcast.title)
                cancel()
            }

            // When
            dao.getPodcasts(query = "Java", limit = 10).test {
                val podcasts = awaitItem()
                // Then
                assertEquals(1, podcasts.size)
                assertEquals("Java Programming", podcasts[0].podcast.title)
                cancel()
            }
        }

    @Test
    fun `Given query, When getPodcastsPaging is called, Then all podcasts are returned`() =
        runTest {
            // Given
            dao.upsertPodcasts(podcastEntities)

            // When
            val podcasts = dao.getPodcastsPaging().loadAsSnapshot()

            // Then
            assertEquals(10, podcasts.size)
        }

    @Test
    fun `Given query, When getPodcastsPaging is called, Then filtered podcasts are returned`() =
        runTest {
            // Given
            dao.upsertPodcasts(
                listOf(
                    podcastEntity.copy(id = 100L, title = "Java Programming"),
                    podcastEntity.copy(id = 101L, title = "Kotlin Programming"),
                )
            )

            // When
            val podcasts = dao.getPodcastsPaging(query = "Kotlin").loadAsSnapshot()

            // Then
            assertEquals(1, podcasts.size)
            assertEquals("Kotlin Programming", podcasts[0].podcast.title)
        }

    @Test
    fun `Given groupKey, When getPodcastsByGroupKey is called, Then podcasts in group are returned`() =
        runTest {
            // Given
            dao.upsertPodcastsWithGroup(podcastEntities.take(3), "test_group")
            dao.upsertPodcastsWithGroup(podcastEntities.drop(3).take(2), "other_group")

            // When
            dao.getPodcastsByGroupKey("test_group", 10).test {
                val podcasts = awaitItem()
                // Then
                assertEquals(3, podcasts.size)
                cancel()
            }
        }

    @Test
    fun `Given groupKey, When getPodcastsByGroupKeyPaging is called, Then podcasts in group are returned`() =
        runTest {
            // Given
            dao.upsertPodcastsWithGroup(podcastEntities.take(3), "test_group")
            dao.upsertPodcastsWithGroup(podcastEntities.drop(3).take(2), "other_group")

            // When
            val podcasts = dao.getPodcastsByGroupKeyPaging("test_group").loadAsSnapshot()

            // Then
            assertEquals(3, podcasts.size)
        }

    @Test
    fun `Given groupKey with podcasts, When getOldestCreatedAtByGroupKey is called, Then oldest timestamp is returned`() =
        runTest {
            // Given
            dao.upsertPodcastsWithGroup(podcastEntities.take(3), "test_group")

            // When
            val oldestCreatedAt = dao.getOldestCreatedAtByGroupKey("test_group")

            // Then
            assertEquals(true, oldestCreatedAt != null)
        }

    @Test
    fun `Given groupKey without podcasts, When getOldestCreatedAtByGroupKey is called, Then null is returned`() =
        runTest {
            // When
            val oldestCreatedAt = dao.getOldestCreatedAtByGroupKey("non_existing_group")

            // Then
            assertEquals(null, oldestCreatedAt)
        }

    @Test
    fun `Given podcasts with groupKey, When replacePodcasts is called, Then old podcasts are replaced`() =
        runTest {
            // Given
            dao.upsertPodcastsWithGroup(podcastEntities.take(3), "test_group")

            // When
            val newPodcasts = podcastEntities.drop(3).take(2)
            dao.replacePodcasts(newPodcasts, "test_group")

            // Then
            dao.getPodcastsByGroupKey("test_group", 10).test {
                val podcasts = awaitItem()
                assertEquals(2, podcasts.size)
                assertEquals(newPodcasts.map { it.id }, podcasts.map { it.podcast.id })
                cancel()
            }
        }

    @Test
    fun `Given podcast id, When addFollowedPodcast is called, Then podcast is followed`() =
        runTest {
            // Given
            dao.upsertPodcast(podcastEntity.copy(id = 100L))

            // When
            dao.addFollowedPodcast(
                FollowedPodcastEntity(
                    id = 100L,
                    followedAt = Instant.fromEpochSeconds(0),
                    isNotificationEnabled = false,
                )
            )

            // Then
            dao.isFollowedPodcast(100L).test {
                val isFollowed = awaitItem()
                assertEquals(true, isFollowed)
                cancel()
            }
        }

    @Test
    fun `Given followed podcast id, When removeFollowedPodcast is called, Then podcast is unfollowed`() =
        runTest {
            // Given
            dao.upsertPodcast(podcastEntity.copy(id = 100L))
            dao.addFollowedPodcast(
                FollowedPodcastEntity(
                    id = 100L,
                    followedAt = Instant.fromEpochSeconds(0),
                    isNotificationEnabled = false,
                )
            )

            // When
            dao.removeFollowedPodcast(100L)

            // Then
            dao.isFollowedPodcast(100L).test {
                val isFollowed = awaitItem()
                assertEquals(false, isFollowed)
                cancel()
            }
        }

    @Test
    fun `Given followed podcast, When isFollowedPodcast is called, Then true is returned`() =
        runTest {
            // Given
            dao.upsertPodcast(podcastEntity.copy(id = 100L))
            dao.addFollowedPodcast(
                FollowedPodcastEntity(
                    id = 100L,
                    followedAt = Instant.fromEpochSeconds(0),
                    isNotificationEnabled = false,
                )
            )

            // When
            dao.isFollowedPodcast(100L).test {
                val isFollowed = awaitItem()
                // Then
                assertEquals(true, isFollowed)
                cancel()
            }
        }

    @Test
    fun `Given unfollowed podcast, When isFollowedPodcast is called, Then false is returned`() =
        runTest {
            // Given
            dao.upsertPodcast(podcastEntity.copy(id = 100L))

            // When
            dao.isFollowedPodcast(100L).test {
                val isFollowed = awaitItem()
                // Then
                assertEquals(false, isFollowed)
                cancel()
            }
        }

    @Test
    fun `Given unfollowed podcast, When toggleFollowedPodcast is called, Then podcast is followed`() =
        runTest {
            // Given
            dao.upsertPodcast(podcastEntity.copy(id = 100L))

            // When
            val result = dao.toggleFollowedPodcast(100L)

            // Then
            assertEquals(true, result)
            dao.isFollowedPodcast(100L).test {
                val isFollowed = awaitItem()
                assertEquals(true, isFollowed)
                cancel()
            }
        }

    @Test
    fun `Given followed podcast, When toggleFollowedPodcast is called, Then podcast is unfollowed`() =
        runTest {
            // Given
            dao.upsertPodcast(podcastEntity.copy(id = 100L))
            dao.addFollowedPodcast(
                FollowedPodcastEntity(
                    id = 100L,
                    followedAt = Instant.fromEpochSeconds(0),
                    isNotificationEnabled = false,
                )
            )

            // When
            val result = dao.toggleFollowedPodcast(100L)

            // Then
            assertEquals(false, result)
            dao.isFollowedPodcast(100L).test {
                val isFollowed = awaitItem()
                assertEquals(false, isFollowed)
                cancel()
            }
        }

    @Test
    fun `Given no query, When getFollowedPodcasts is called, Then all followed podcasts are returned`() =
        runTest {
            // Given
            dao.upsertPodcasts(podcastEntities.take(3))
            podcastEntities.take(3).forEach {
                dao.addFollowedPodcast(
                    FollowedPodcastEntity(
                        id = it.id,
                        followedAt = Instant.fromEpochSeconds(0),
                        isNotificationEnabled = false,
                    )
                )
            }

            // When
            dao.getFollowedPodcasts(limit = 10).test {
                val podcasts = awaitItem()
                // Then
                assertEquals(3, podcasts.size)
                cancel()
            }
        }

    @Test
    fun `Given query, When getFollowedPodcasts is called, Then filtered followed podcasts are returned`() =
        runTest {
            // Given
            dao.upsertPodcasts(
                listOf(
                    podcastEntity.copy(id = 100L, title = "Kotlin Programming"),
                    podcastEntity.copy(id = 101L, title = "Java Programming"),
                )
            )
            dao.addFollowedPodcast(
                FollowedPodcastEntity(
                    id = 100L,
                    followedAt = Instant.fromEpochSeconds(0),
                    isNotificationEnabled = false,
                )
            )
            dao.addFollowedPodcast(
                FollowedPodcastEntity(
                    id = 101L,
                    followedAt = Instant.fromEpochSeconds(0),
                    isNotificationEnabled = false,
                )
            )

            // When
            dao.getFollowedPodcasts(query = "Kotlin", limit = 10).test {
                val podcasts = awaitItem()
                // Then
                assertEquals(1, podcasts.size)
                assertEquals("Kotlin Programming", podcasts[0].podcast.title)
                cancel()
            }
        }

    @Test
    fun `Given query, When getFollowedPodcastsPaging is called, Then filtered followed podcasts are returned`() =
        runTest {
            // Given
            dao.upsertPodcasts(
                listOf(
                    podcastEntity.copy(id = 100L, title = "Kotlin Programming"),
                    podcastEntity.copy(id = 101L, title = "Java Programming"),
                )
            )
            dao.addFollowedPodcast(
                FollowedPodcastEntity(
                    id = 100L,
                    followedAt = Instant.fromEpochSeconds(0),
                    isNotificationEnabled = false,
                )
            )
            dao.addFollowedPodcast(
                FollowedPodcastEntity(
                    id = 101L,
                    followedAt = Instant.fromEpochSeconds(0),
                    isNotificationEnabled = false,
                )
            )

            // When
            val podcasts = dao.getFollowedPodcastsPaging(query = "Kotlin").loadAsSnapshot()

            // Then
            assertEquals(1, podcasts.size)
            assertEquals("Kotlin Programming", podcasts[0].podcast.title)
        }

}