package io.jacob.episodive.core.database.dao

import app.cash.turbine.test
import io.jacob.episodive.core.database.RoomDatabaseRule
import io.jacob.episodive.core.database.mapper.toEpisodeEntities
import io.jacob.episodive.core.database.mapper.toEpisodeEntity
import io.jacob.episodive.core.database.model.EpisodeGroupEntity
import io.jacob.episodive.core.database.model.LikedEpisodeEntity
import io.jacob.episodive.core.database.model.PlayedEpisodeEntity
import io.jacob.episodive.core.database.model.SavedEpisodeEntity
import io.jacob.episodive.core.model.DownloadStatus
import io.jacob.episodive.core.testing.model.episodeTestData
import io.jacob.episodive.core.testing.model.episodeTestDataList
import io.jacob.episodive.core.testing.util.MainDispatcherRule
import io.jacob.episodive.core.testing.util.loadAsSnapshot
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Instant

@RunWith(RobolectricTestRunner::class)
class EpisodeDaoTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @get:Rule
    val dbRule = RoomDatabaseRule()

    private lateinit var dao: EpisodeDao

    @Before
    fun setup() {
        dao = dbRule.db.episodeDao()
    }

    private val episodeEntity = episodeTestData.toEpisodeEntity()
    private val episodeEntities = episodeTestDataList.toEpisodeEntities()

    @Test
    fun `Given single episode, When upsertEpisode is called, Then episode is inserted`() =
        runTest {
            // Given
            dao.upsertEpisode(episodeEntity.copy(id = 100L, title = "aaa"))

            // When
            dao.getEpisodes(limit = 10).test {
                val episodes = awaitItem()

                // Then
                assertEquals(1, episodes.size)
                assertEquals(100L, episodes[0].episode.id)
                assertEquals("aaa", episodes[0].episode.title)

                cancel()
            }
        }

    @Test
    fun `Given single episode, When upsertEpisode is called, Then episode is updated`() =
        runTest {
            // Given
            dao.upsertEpisode(episodeEntity.copy(id = 100L, title = "aaa"))
            dao.upsertEpisode(episodeEntity.copy(id = 100L, title = "bbb"))

            // When
            dao.getEpisodes(limit = 10).test {
                val episodes = awaitItem()

                // Then
                assertEquals(1, episodes.size)
                assertEquals(100L, episodes[0].episode.id)
                assertEquals("bbb", episodes[0].episode.title)

                cancel()
            }
        }

    @Test
    fun `Given multiple episodes, When upsertEpisodes is called, Then episodes are upserted`() =
        runTest {
            // Given
            dao.upsertEpisodes(episodeEntities)
            dao.upsertEpisodes(episodeEntities.toMutableList().apply {
                removeAt(0)
                add(episodeEntity.copy(id = 100L, datePublished = Instant.fromEpochSeconds(1)))
            })

            // When
            dao.getEpisodes(limit = 100).test {
                val episodes = awaitItem()

                // Then
                assertEquals(11, episodes.size)
                assertEquals(
                    episodeEntities.map { it.id },
                    episodes.subList(0, 10).map { it.episode.id })
                assertEquals(100L, episodes.last().episode.id)

                cancel()
            }
        }

    @Test
    fun `Given single episodeGroup, When upsertEpisodeGroup is called, Then episodeGroup is upserted`() =
        runTest {
            dao.upsertEpisode(episodeEntities[0])
            // Given
            val group = EpisodeGroupEntity(
                groupKey = "test_group",
                id = episodeEntities[0].id,
                order = 0,
                createdAt = Instant.fromEpochSeconds(0),
            )
            dao.upsertEpisodeGroup(group)
            dao.upsertEpisodeGroup(group.copy(order = 1))


            // When
            val groups = dao.getEpisodeGroupsByGroupKey("test_group")

            // Then
            assertEquals(1, groups.size)
            assertEquals(1, groups[0].order)
        }

    @Test
    fun `Given multiple episodeGroups, When upsertEpisodeGroups is called, Then episodeGroups are upserted`() =
        runTest {
            // Given
            dao.upsertEpisodes(episodeEntities.take(3))
            val groups = episodeEntities.take(3).mapIndexed { index, episode ->
                EpisodeGroupEntity(
                    groupKey = "test_group",
                    id = episode.id,
                    order = index,
                    createdAt = Instant.fromEpochSeconds(0),
                )
            }

            // When
            dao.upsertEpisodeGroups(groups)

            // Then
            val result = dao.getEpisodeGroupsByGroupKey("test_group")
            assertEquals(3, result.size)
            assertEquals(listOf(0, 1, 2), result.map { it.order })
        }

    @Test
    fun `Given episodes and groupKey, When upsertEpisodesWithGroup is called, Then episodes and groups are upserted`() =
        runTest {
            // Given
            val episodes = episodeEntities.take(3)

            // When
            dao.upsertEpisodesWithGroup(episodes, "test_group")

            // Then
            dao.getEpisodesByGroupKey("test_group", 10).test {
                val result = awaitItem()
                assertEquals(3, result.size)
                assertEquals(episodes.map { it.id }, result.map { it.episode.id })
                cancel()
            }
        }

    @Test
    fun `Given episode id, When deleteEpisode is called, Then episode is deleted`() =
        runTest {
            // Given
            dao.upsertEpisode(episodeEntity.copy(id = 100L))

            // When
            dao.deleteEpisode(100L)

            // Then
            dao.getEpisodes(limit = 10).test {
                val episodes = awaitItem()
                assertEquals(0, episodes.size)
                cancel()
            }
        }

    @Test
    fun `Given multiple episodes, When deleteEpisodes is called, Then all episodes are deleted`() =
        runTest {
            // Given
            dao.upsertEpisodes(episodeEntities)

            // When
            dao.deleteEpisodes()

            // Then
            dao.getEpisodes(limit = 100).test {
                val episodes = awaitItem()
                assertEquals(0, episodes.size)
                cancel()
            }
        }

    @Test
    fun `Given orphaned episode, When deleteEpisodesIfOrphaned is called, Then episode is deleted`() =
        runTest {
            // Given
            dao.upsertEpisodes(episodeEntities.take(2))

            // When
            dao.deleteEpisodesIfOrphaned(episodeEntities.take(2).map { it.id })

            // Then
            dao.getEpisodes(limit = 10).test {
                val episodes = awaitItem()
                assertEquals(0, episodes.size)
                cancel()
            }
        }

    @Test
    fun `Given liked episode, When deleteEpisodesIfOrphaned is called, Then episode is not deleted`() =
        runTest {
            // Given
            dao.upsertEpisode(episodeEntity.copy(id = 100L))
            dao.addLikedEpisode(
                LikedEpisodeEntity(
                    id = 100L,
                    likedAt = Instant.fromEpochSeconds(0),
                )
            )

            // When
            dao.deleteEpisodesIfOrphaned(listOf(100L))

            // Then
            dao.getEpisodes(limit = 10).test {
                val episodes = awaitItem()
                assertEquals(1, episodes.size)
                assertEquals(100L, episodes[0].episode.id)
                cancel()
            }
        }

    @Test
    fun `Given played episode, When deleteEpisodesIfOrphaned is called, Then episode is not deleted`() =
        runTest {
            // Given
            dao.upsertEpisode(episodeEntity.copy(id = 100L))
            dao.upsertPlayedEpisode(
                PlayedEpisodeEntity(
                    id = 100L,
                    playedAt = Instant.fromEpochSeconds(0),
                    position = Duration.ZERO,
                    isCompleted = false,
                )
            )

            // When
            dao.deleteEpisodesIfOrphaned(listOf(100L))

            // Then
            dao.getEpisodes(limit = 10).test {
                val episodes = awaitItem()
                assertEquals(1, episodes.size)
                assertEquals(100L, episodes[0].episode.id)
                cancel()
            }
        }

    @Test
    fun `Given episode in group, When deleteEpisodesIfOrphaned is called, Then episode is not deleted`() =
        runTest {
            // Given
            dao.upsertEpisodesWithGroup(listOf(episodeEntity.copy(id = 100L)), "test_group")

            // When
            dao.deleteEpisodesIfOrphaned(listOf(100L))

            // Then
            dao.getEpisodes(limit = 10).test {
                val episodes = awaitItem()
                assertEquals(1, episodes.size)
                assertEquals(100L, episodes[0].episode.id)
                cancel()
            }
        }

    @Test
    fun `Given groupKey, When deleteEpisodeGroupsByGroupKey is called, Then groups are deleted`() =
        runTest {
            // Given
            dao.upsertEpisodesWithGroup(episodeEntities.take(3), "test_group")

            // When
            dao.deleteEpisodeGroupsByGroupKey("test_group")

            // Then
            val groups = dao.getEpisodeGroupsByGroupKey("test_group")
            assertEquals(0, groups.size)
        }

    @Test
    fun `Given episode id and duration, When updateEpisodeDuration is called, Then duration is updated`() =
        runTest {
            // Given
            dao.upsertEpisode(episodeEntity.copy(id = 100L, duration = Duration.ZERO))

            // When
            dao.updateEpisodeDuration(100L, 1.hours)

            // Then
            dao.getEpisodeById(100L).test {
                val episode = awaitItem()
                assertEquals(1.hours, episode?.episode?.duration!!)
                cancel()
            }
        }

    @Test
    fun `Given existing episode id, When getEpisodeById is called, Then episode is returned`() =
        runTest {
            // Given
            dao.upsertEpisode(episodeEntity.copy(id = 100L))

            // When
            dao.getEpisodeById(100L).test {
                val episode = awaitItem()
                // Then
                assertEquals(100L, episode?.episode?.id)
                cancel()
            }
        }

    @Test
    fun `Given non-existing episode id, When getEpisodeById is called, Then null is returned`() =
        runTest {
            // When
            dao.getEpisodeById(999L).test {
                val episode = awaitItem()
                // Then
                assertEquals(null, episode)
                cancel()
            }
        }

    @Test
    fun `Given episode ids, When getEpisodesByIds is called, Then episodes are returned`() =
        runTest {
            // Given
            dao.upsertEpisodes(episodeEntities.take(3))
            val ids = episodeEntities.take(3).map { it.id }

            // When
            dao.getEpisodesByIds(ids).test {
                val episodes = awaitItem()
                // Then
                assertEquals(3, episodes.size)
                assertEquals(ids, episodes.map { it.episode.id })
                cancel()
            }
        }

    @Test
    fun `Given no query, When getEpisodes is called, Then all episodes are returned`() =
        runTest {
            // Given
            dao.upsertEpisodes(episodeEntities)

            // When
            dao.getEpisodes(limit = 100).test {
                val episodes = awaitItem()
                // Then
                assertEquals(10, episodes.size)
                cancel()
            }
        }

    @Test
    fun `Given query, When getEpisodes is called, Then filtered episodes are returned`() =
        runTest {
            // Given
            dao.upsertEpisodes(
                listOf(
                    episodeEntity.copy(id = 100L, title = "Java Programming"),
                    episodeEntity.copy(id = 101L, title = "Kotlin Programming"),
                )
            )

            // When
            dao.getEpisodes(query = "Kotlin", limit = 10).test {
                val episodes = awaitItem()
                // Then
                assertEquals(1, episodes.size)
                assertEquals("Kotlin Programming", episodes[0].episode.title)
                cancel()
            }

            // When
            dao.getEpisodes(query = "Java", limit = 10).test {
                val episodes = awaitItem()
                // Then
                assertEquals(1, episodes.size)
                assertEquals("Java Programming", episodes[0].episode.title)
                cancel()
            }
        }

    @Test
    fun `Given query, When getEpisodesPaging is called, Then all episodes are returned`() =
        runTest {
            // Given
            dao.upsertEpisodes(episodeEntities)

            // When
            val episodes = dao.getEpisodesPaging().loadAsSnapshot()

            // Then
            assertEquals(10, episodes.size)
        }

    @Test
    fun `Given query, When getEpisodesPaging is called, Then filtered episodes are returned`() =
        runTest {
            // Given
            dao.upsertEpisodes(
                listOf(
                    episodeEntity.copy(id = 100L, title = "Java Programming"),
                    episodeEntity.copy(id = 101L, title = "Kotlin Programming"),
                )
            )

            // When
            val episodes = dao.getEpisodesPaging(query = "Kotlin").loadAsSnapshot()

            // Then
            assertEquals(1, episodes.size)
            assertEquals("Kotlin Programming", episodes[0].episode.title)
        }

    @Test
    fun `Given groupKey, When getEpisodesByGroupKey is called, Then episodes in group are returned`() =
        runTest {
            // Given
            dao.upsertEpisodesWithGroup(episodeEntities.take(3), "test_group")
            dao.upsertEpisodesWithGroup(episodeEntities.drop(3).take(2), "other_group")

            // When
            dao.getEpisodesByGroupKey("test_group", 10).test {
                val episodes = awaitItem()
                // Then
                assertEquals(3, episodes.size)
                cancel()
            }
        }

    @Test
    fun `Given groupKey, When getEpisodesByGroupKeyPaging is called, Then episodes in group are returned`() =
        runTest {
            // Given
            dao.upsertEpisodesWithGroup(episodeEntities.take(3), "test_group")
            dao.upsertEpisodesWithGroup(episodeEntities.drop(3).take(2), "other_group")

            // When
            val episodes = dao.getEpisodesByGroupKeyPaging("test_group").loadAsSnapshot()

            // Then
            assertEquals(3, episodes.size)
        }

    @Test
    fun `Given groupKey with episodes, When getOldestCreatedAtByGroupKey is called, Then oldest timestamp is returned`() =
        runTest {
            // Given
            dao.upsertEpisodesWithGroup(episodeEntities.take(3), "test_group")

            // When
            val oldestCreatedAt = dao.getOldestCreatedAtByGroupKey("test_group")

            // Then
            assertEquals(true, oldestCreatedAt != null)
        }

    @Test
    fun `Given groupKey without episodes, When getOldestCreatedAtByGroupKey is called, Then null is returned`() =
        runTest {
            // When
            val oldestCreatedAt = dao.getOldestCreatedAtByGroupKey("non_existing_group")

            // Then
            assertEquals(null, oldestCreatedAt)
        }

    @Test
    fun `Given no episode groups, When getEpisodeGroupCount is called, Then zero is returned`() =
        runTest {
            // When
            val count = dao.getEpisodeGroupCount()

            // Then
            assertEquals(0, count)
        }

    @Test
    fun `Given episode groups, When getEpisodeGroupCount is called without prefix, Then total count is returned`() =
        runTest {
            // Given
            dao.upsertEpisodesWithGroup(episodeEntities.take(3), "trending:feed1")
            dao.upsertEpisodesWithGroup(episodeEntities.drop(3).take(2), "trending:feed2")
            dao.upsertEpisodesWithGroup(episodeEntities.drop(5).take(2), "recent:feed1")

            // When
            val count = dao.getEpisodeGroupCount()

            // Then
            assertEquals(7, count)
        }

    @Test
    fun `Given episode groups, When getEpisodeGroupCount is called with prefix, Then filtered count is returned`() =
        runTest {
            // Given
            dao.upsertEpisodesWithGroup(episodeEntities.take(3), "trending:feed1")
            dao.upsertEpisodesWithGroup(episodeEntities.drop(3).take(2), "trending:feed2")
            dao.upsertEpisodesWithGroup(episodeEntities.drop(5).take(2), "recent:feed1")

            // When
            val trendingCount = dao.getEpisodeGroupCount(prefix = "trending")
            val recentCount = dao.getEpisodeGroupCount(prefix = "recent")

            // Then
            assertEquals(5, trendingCount)
            assertEquals(2, recentCount)
        }

    @Test
    fun `Given no episode groups, When getGroupKeysWithCounts is called, Then empty list is returned`() =
        runTest {
            // When
            val groupKeys = dao.getGroupKeysWithCounts()

            // Then
            assertEquals(0, groupKeys.size)
        }

    @Test
    fun `Given episode groups, When getGroupKeysWithCounts is called without prefix, Then all groups with counts are returned`() =
        runTest {
            // Given
            dao.upsertEpisodes(episodeEntities.take(7))
            val groups = listOf(
                // trending:feed1 - 3 episodes (oldest)
                EpisodeGroupEntity(
                    "trending:feed1",
                    episodeEntities[0].id,
                    0,
                    Instant.fromEpochSeconds(100)
                ),
                EpisodeGroupEntity(
                    "trending:feed1",
                    episodeEntities[1].id,
                    1,
                    Instant.fromEpochSeconds(100)
                ),
                EpisodeGroupEntity(
                    "trending:feed1",
                    episodeEntities[2].id,
                    2,
                    Instant.fromEpochSeconds(100)
                ),
                // trending:feed2 - 2 episodes
                EpisodeGroupEntity(
                    "trending:feed2",
                    episodeEntities[3].id,
                    0,
                    Instant.fromEpochSeconds(200)
                ),
                EpisodeGroupEntity(
                    "trending:feed2",
                    episodeEntities[4].id,
                    1,
                    Instant.fromEpochSeconds(200)
                ),
                // recent:feed1 - 2 episodes (newest)
                EpisodeGroupEntity(
                    "recent:feed1",
                    episodeEntities[5].id,
                    0,
                    Instant.fromEpochSeconds(300)
                ),
                EpisodeGroupEntity(
                    "recent:feed1",
                    episodeEntities[6].id,
                    1,
                    Instant.fromEpochSeconds(300)
                ),
            )
            dao.upsertEpisodeGroups(groups)

            // When
            val groupKeys = dao.getGroupKeysWithCounts()

            // Then
            assertEquals(3, groupKeys.size)
            assertEquals("trending:feed1", groupKeys[0].groupKey)
            assertEquals(3, groupKeys[0].count)
            assertEquals("trending:feed2", groupKeys[1].groupKey)
            assertEquals(2, groupKeys[1].count)
            assertEquals("recent:feed1", groupKeys[2].groupKey)
            assertEquals(2, groupKeys[2].count)
        }

    @Test
    fun `Given episode groups, When getGroupKeysWithCounts is called with prefix, Then filtered groups with counts are returned`() =
        runTest {
            // Given
            dao.upsertEpisodesWithGroup(episodeEntities.take(3), "trending:feed1")
            dao.upsertEpisodesWithGroup(episodeEntities.drop(3).take(2), "trending:feed2")
            dao.upsertEpisodesWithGroup(episodeEntities.drop(5).take(2), "recent:feed1")

            // When
            val trendingGroups = dao.getGroupKeysWithCounts(prefix = "trending")
            val recentGroups = dao.getGroupKeysWithCounts(prefix = "recent")

            // Then
            assertEquals(2, trendingGroups.size)
            assertEquals("trending:feed1", trendingGroups[0].groupKey)
            assertEquals(3, trendingGroups[0].count)
            assertEquals("trending:feed2", trendingGroups[1].groupKey)
            assertEquals(2, trendingGroups[1].count)

            assertEquals(1, recentGroups.size)
            assertEquals("recent:feed1", recentGroups[0].groupKey)
            assertEquals(2, recentGroups[0].count)
        }

    @Test
    fun `Given no episode groups, When getEpisodeIdsByGroupKeys is called, Then empty list is returned`() =
        runTest {
            // When
            val episodeIds = dao.getEpisodeIdsByGroupKeys(listOf("non_existing_group"))

            // Then
            assertEquals(0, episodeIds.size)
        }

    @Test
    fun `Given episode groups, When getEpisodeIdsByGroupKeys is called, Then episode ids are returned`() =
        runTest {
            // Given
            dao.upsertEpisodesWithGroup(episodeEntities.take(3), "group1")
            dao.upsertEpisodesWithGroup(episodeEntities.drop(3).take(2), "group2")

            // When
            val episodeIds = dao.getEpisodeIdsByGroupKeys(listOf("group1", "group2"))

            // Then
            assertEquals(5, episodeIds.size)
            assertEquals(episodeEntities.take(5).map { it.id }.toSet(), episodeIds.toSet())
        }

    @Test
    fun `Given episode groups, When getEpisodeIdsByGroupKeys is called with single group, Then only that group's episode ids are returned`() =
        runTest {
            // Given
            dao.upsertEpisodesWithGroup(episodeEntities.take(3), "group1")
            dao.upsertEpisodesWithGroup(episodeEntities.drop(3).take(2), "group2")

            // When
            val episodeIds = dao.getEpisodeIdsByGroupKeys(listOf("group1"))

            // Then
            assertEquals(3, episodeIds.size)
            assertEquals(episodeEntities.take(3).map { it.id }.toSet(), episodeIds.toSet())
        }

    @Test
    fun `Given episode groups, When deleteEpisodeGroupsByGroupKeys is called, Then specified groups are deleted`() =
        runTest {
            // Given
            dao.upsertEpisodesWithGroup(episodeEntities.take(3), "group1")
            dao.upsertEpisodesWithGroup(episodeEntities.drop(3).take(2), "group2")
            dao.upsertEpisodesWithGroup(episodeEntities.drop(5).take(2), "group3")

            // When
            dao.deleteEpisodeGroupsByGroupKeys(listOf("group1", "group3"))

            // Then
            val group1 = dao.getEpisodeGroupsByGroupKey("group1")
            val group2 = dao.getEpisodeGroupsByGroupKey("group2")
            val group3 = dao.getEpisodeGroupsByGroupKey("group3")

            assertEquals(0, group1.size)
            assertEquals(2, group2.size)
            assertEquals(0, group3.size)
        }

    @Test
    fun `Given total count below threshold, When deleteOldestGroupsIfExceedsLimit is called, Then no groups are deleted`() =
        runTest {
            // Given
            dao.upsertEpisodesWithGroup(episodeEntities.take(3), "trending:feed1")
            dao.upsertEpisodesWithGroup(episodeEntities.drop(3).take(2), "trending:feed2")

            // When
            dao.deleteOldestGroupsIfExceedsLimit(
                threshold = 10,
                targetCount = 5,
                prefix = "trending"
            )

            // Then
            val count = dao.getEpisodeGroupCount(prefix = "trending")
            assertEquals(5, count)
        }

    @Test
    fun `Given total count exceeds threshold, When deleteOldestGroupsIfExceedsLimit is called, Then oldest groups are deleted`() =
        runTest {
            // Given
            dao.upsertEpisodesWithGroup(episodeEntities.take(2), "trending:feed1")
            dao.upsertEpisodesWithGroup(episodeEntities.drop(2).take(2), "trending:feed2")
            dao.upsertEpisodesWithGroup(episodeEntities.drop(4).take(2), "trending:feed3")
            dao.upsertEpisodesWithGroup(episodeEntities.drop(6).take(2), "trending:feed4")

            // When
            dao.deleteOldestGroupsIfExceedsLimit(
                threshold = 5,
                targetCount = 3,
                prefix = "trending"
            )

            // Then
            val count = dao.getEpisodeGroupCount(prefix = "trending")
            val groups = dao.getGroupKeysWithCounts(prefix = "trending")

            assertEquals(2, count)
            assertEquals(1, groups.size)
            // Oldest groups (feed1, feed2, feed3) should be deleted, only feed4 remains
            assertEquals("trending:feed4", groups[0].groupKey)
        }

    @Test
    fun `Given total count exceeds threshold without prefix, When deleteOldestGroupsIfExceedsLimit is called, Then oldest groups are deleted`() =
        runTest {
            // Given
            dao.upsertEpisodesWithGroup(episodeEntities.take(2), "feed1")
            dao.upsertEpisodesWithGroup(episodeEntities.drop(2).take(2), "feed2")
            dao.upsertEpisodesWithGroup(episodeEntities.drop(4).take(2), "feed3")

            // When
            dao.deleteOldestGroupsIfExceedsLimit(threshold = 4, targetCount = 2)

            // Then
            val count = dao.getEpisodeGroupCount()
            val groups = dao.getGroupKeysWithCounts()

            assertEquals(2, count)
            assertEquals(1, groups.size)
            // Oldest groups (feed1, feed2) should be deleted
            assertEquals("feed3", groups[0].groupKey)
        }

    @Test
    fun `Given orphaned episodes after group deletion, When deleteOldestGroupsIfExceedsLimit is called, Then orphaned episodes are deleted`() =
        runTest {
            // Given
            dao.upsertEpisodesWithGroup(episodeEntities.take(2), "trending:feed1")
            dao.upsertEpisodesWithGroup(episodeEntities.drop(2).take(2), "trending:feed2")

            // When
            dao.deleteOldestGroupsIfExceedsLimit(
                threshold = 2,
                targetCount = 2,
                prefix = "trending"
            )

            // Then
            val groupCount = dao.getEpisodeGroupCount(prefix = "trending")
            assertEquals(2, groupCount)

            // Verify orphaned episodes are deleted
            dao.getEpisodes(limit = 100).test {
                val episodes = awaitItem()
                assertEquals(2, episodes.size)
                // Only feed2 episodes should remain
                assertEquals(
                    episodeEntities.drop(2).take(2).map { it.id }.toSet(),
                    episodes.map { it.episode.id }.toSet()
                )
                cancel()
            }
        }

    @Test
    fun `Given liked episodes in oldest groups, When deleteOldestGroupsIfExceedsLimit is called, Then liked episodes are not deleted`() =
        runTest {
            // Given
            dao.upsertEpisodesWithGroup(episodeEntities.take(2), "trending:feed1")
            dao.upsertEpisodesWithGroup(episodeEntities.drop(2).take(2), "trending:feed2")

            // Like first episode
            dao.addLikedEpisode(
                LikedEpisodeEntity(
                    id = episodeEntities[0].id,
                    likedAt = Instant.fromEpochSeconds(0),
                )
            )

            // When
            dao.deleteOldestGroupsIfExceedsLimit(
                threshold = 2,
                targetCount = 2,
                prefix = "trending"
            )

            // Then
            dao.getEpisodes(limit = 100).test {
                val episodes = awaitItem()
                // Liked episode from feed1 + 2 episodes from feed2 = 3 episodes
                assertEquals(3, episodes.size)
                cancel()
            }
        }

    @Test
    fun `Given episodes with groupKey, When replaceEpisodes is called, Then old episodes are replaced`() =
        runTest {
            // Given
            dao.upsertEpisodesWithGroup(episodeEntities.take(3), "test_group")

            // When
            val newEpisodes = episodeEntities.drop(3).take(2)
            dao.replaceEpisodes(newEpisodes, "test_group")

            // Then
            dao.getEpisodesByGroupKey("test_group", 10).test {
                val episodes = awaitItem()
                assertEquals(2, episodes.size)
                assertEquals(newEpisodes.map { it.id }, episodes.map { it.episode.id })
                cancel()
            }
        }

    @Test
    fun `Given episode id, When addLikedEpisode is called, Then episode is liked`() =
        runTest {
            // Given
            dao.upsertEpisode(episodeEntity.copy(id = 100L))

            // When
            dao.addLikedEpisode(
                LikedEpisodeEntity(
                    id = 100L,
                    likedAt = Instant.fromEpochSeconds(0),
                )
            )

            // Then
            dao.isLikedEpisode(100L).test {
                val isLiked = awaitItem()
                assertEquals(true, isLiked)
                cancel()
            }
        }

    @Test
    fun `Given liked episode id, When removeLikedEpisode is called, Then episode is unliked`() =
        runTest {
            // Given
            dao.upsertEpisode(episodeEntity.copy(id = 100L))
            dao.addLikedEpisode(
                LikedEpisodeEntity(
                    id = 100L,
                    likedAt = Instant.fromEpochSeconds(0),
                )
            )

            // When
            dao.removeLikedEpisode(100L)

            // Then
            dao.isLikedEpisode(100L).test {
                val isLiked = awaitItem()
                assertEquals(false, isLiked)
                cancel()
            }
        }

    @Test
    fun `Given liked episode, When isLikedEpisode is called, Then true is returned`() =
        runTest {
            // Given
            dao.upsertEpisode(episodeEntity.copy(id = 100L))
            dao.addLikedEpisode(
                LikedEpisodeEntity(
                    id = 100L,
                    likedAt = Instant.fromEpochSeconds(0),
                )
            )

            // When
            dao.isLikedEpisode(100L).test {
                val isLiked = awaitItem()
                // Then
                assertEquals(true, isLiked)
                cancel()
            }
        }

    @Test
    fun `Given unliked episode, When isLikedEpisode is called, Then false is returned`() =
        runTest {
            // Given
            dao.upsertEpisode(episodeEntity.copy(id = 100L))

            // When
            dao.isLikedEpisode(100L).test {
                val isLiked = awaitItem()
                // Then
                assertEquals(false, isLiked)
                cancel()
            }
        }

    @Test
    fun `Given unliked episode, When toggleLikedEpisode is called, Then episode is liked`() =
        runTest {
            // Given
            val episode = episodeEntity.copy(id = 100L)
            dao.upsertEpisode(episode)

            // When
            val result = dao.toggleLikedEpisode(episode)

            // Then
            assertEquals(true, result)
            dao.isLikedEpisode(100L).test {
                val isLiked = awaitItem()
                assertEquals(true, isLiked)
                cancel()
            }
        }

    @Test
    fun `Given liked episode, When toggleLikedEpisode is called, Then episode is unliked`() =
        runTest {
            // Given
            val episode = episodeEntity.copy(id = 100L)
            dao.upsertEpisode(episode)
            dao.addLikedEpisode(
                LikedEpisodeEntity(
                    id = 100L,
                    likedAt = Instant.fromEpochSeconds(0),
                )
            )

            // When
            val result = dao.toggleLikedEpisode(episode)

            // Then
            assertEquals(false, result)
            dao.isLikedEpisode(100L).test {
                val isLiked = awaitItem()
                assertEquals(false, isLiked)
                cancel()
            }
        }

    @Test
    fun `Given no query, When getLikedEpisodes is called, Then all liked episodes are returned`() =
        runTest {
            // Given
            dao.upsertEpisodes(episodeEntities.take(3))
            episodeEntities.take(3).forEach {
                dao.addLikedEpisode(
                    LikedEpisodeEntity(
                        id = it.id,
                        likedAt = Instant.fromEpochSeconds(0),
                    )
                )
            }

            // When
            dao.getLikedEpisodes(limit = 10).test {
                val episodes = awaitItem()
                // Then
                assertEquals(3, episodes.size)
                cancel()
            }
        }

    @Test
    fun `Given query, When getLikedEpisodes is called, Then filtered liked episodes are returned`() =
        runTest {
            // Given
            dao.upsertEpisodes(
                listOf(
                    episodeEntity.copy(id = 100L, title = "Kotlin Programming"),
                    episodeEntity.copy(id = 101L, title = "Java Programming"),
                )
            )
            dao.addLikedEpisode(
                LikedEpisodeEntity(
                    id = 100L,
                    likedAt = Instant.fromEpochSeconds(0)
                )
            )
            dao.addLikedEpisode(
                LikedEpisodeEntity(
                    id = 101L,
                    likedAt = Instant.fromEpochSeconds(0)
                )
            )

            // When
            dao.getLikedEpisodes(query = "Kotlin", limit = 10).test {
                val episodes = awaitItem()
                // Then
                assertEquals(1, episodes.size)
                assertEquals("Kotlin Programming", episodes[0].episode.title)
                cancel()
            }
        }

    @Test
    fun `Given query, When getLikedEpisodesPaging is called, Then filtered liked episodes are returned`() =
        runTest {
            // Given
            dao.upsertEpisodes(
                listOf(
                    episodeEntity.copy(id = 100L, title = "Kotlin Programming"),
                    episodeEntity.copy(id = 101L, title = "Java Programming"),
                )
            )
            dao.addLikedEpisode(
                LikedEpisodeEntity(
                    id = 100L,
                    likedAt = Instant.fromEpochSeconds(0)
                )
            )
            dao.addLikedEpisode(
                LikedEpisodeEntity(
                    id = 101L,
                    likedAt = Instant.fromEpochSeconds(0)
                )
            )

            // When
            val episodes = dao.getLikedEpisodesPaging(query = "Kotlin").loadAsSnapshot()

            // Then
            assertEquals(1, episodes.size)
            assertEquals("Kotlin Programming", episodes[0].episode.title)
        }

    @Test
    fun `Given played episode, When upsertPlayedEpisode is called, Then episode is marked as played`() =
        runTest {
            // Given
            dao.upsertEpisode(episodeEntity.copy(id = 100L))

            // When
            dao.upsertPlayedEpisode(
                PlayedEpisodeEntity(
                    id = 100L,
                    playedAt = Instant.fromEpochSeconds(0),
                    position = Duration.ZERO,
                    isCompleted = false,
                )
            )

            // Then
            dao.getPlayedEpisodes(limit = 10).test {
                val episodes = awaitItem()
                assertEquals(1, episodes.size)
                assertEquals(100L, episodes[0].episode.id)
                cancel()
            }
        }

    @Test
    fun `Given played episode id, When deletePlayedEpisode is called, Then played episode is deleted`() =
        runTest {
            // Given
            dao.upsertEpisode(episodeEntity.copy(id = 100L))
            dao.upsertPlayedEpisode(
                PlayedEpisodeEntity(
                    id = 100L,
                    playedAt = Instant.fromEpochSeconds(0),
                    position = Duration.ZERO,
                    isCompleted = false,
                )
            )

            // When
            dao.deletePlayedEpisode(100L)

            // Then
            dao.getPlayedEpisodes(limit = 10).test {
                val episodes = awaitItem()
                assertEquals(0, episodes.size)
                cancel()
            }
        }

    @Test
    fun `Given played episode id, When removePlayedEpisode is called, Then played episode and orphaned episode are deleted`() =
        runTest {
            // Given
            dao.upsertEpisode(episodeEntity.copy(id = 100L))
            dao.upsertPlayedEpisode(
                PlayedEpisodeEntity(
                    id = 100L,
                    playedAt = Instant.fromEpochSeconds(0),
                    position = Duration.ZERO,
                    isCompleted = false,
                )
            )

            // When
            dao.removePlayedEpisode(100L)

            // Then
            dao.getPlayedEpisodes(limit = 10).test {
                val playedEpisodes = awaitItem()
                assertEquals(0, playedEpisodes.size)
                cancel()
            }
            dao.getEpisodes(limit = 10).test {
                val episodes = awaitItem()
                assertEquals(0, episodes.size)
                cancel()
            }
        }

    @Test
    fun `Given isCompleted is null, When getPlayedEpisodes is called, Then all played episodes are returned`() =
        runTest {
            // Given
            dao.upsertEpisodes(episodeEntities.take(3))
            dao.upsertPlayedEpisode(
                PlayedEpisodeEntity(
                    id = episodeEntities[0].id,
                    playedAt = Instant.fromEpochSeconds(0),
                    position = Duration.ZERO,
                    isCompleted = true,
                )
            )
            dao.upsertPlayedEpisode(
                PlayedEpisodeEntity(
                    id = episodeEntities[1].id,
                    playedAt = Instant.fromEpochSeconds(0),
                    position = Duration.ZERO,
                    isCompleted = false,
                )
            )

            // When
            dao.getPlayedEpisodes(isCompleted = null, limit = 10).test {
                val episodes = awaitItem()
                // Then
                assertEquals(2, episodes.size)
                cancel()
            }
        }

    @Test
    fun `Given isCompleted is true, When getPlayedEpisodes is called, Then completed episodes are returned`() =
        runTest {
            // Given
            dao.upsertEpisodes(episodeEntities.take(3))
            dao.upsertPlayedEpisode(
                PlayedEpisodeEntity(
                    id = episodeEntities[0].id,
                    playedAt = Instant.fromEpochSeconds(0),
                    position = Duration.ZERO,
                    isCompleted = true,
                )
            )
            dao.upsertPlayedEpisode(
                PlayedEpisodeEntity(
                    id = episodeEntities[1].id,
                    playedAt = Instant.fromEpochSeconds(0),
                    position = Duration.ZERO,
                    isCompleted = false,
                )
            )

            // When
            dao.getPlayedEpisodes(isCompleted = true, limit = 10).test {
                val episodes = awaitItem()
                // Then
                assertEquals(1, episodes.size)
                assertEquals(true, episodes[0].isCompleted)
                cancel()
            }
        }

    @Test
    fun `Given isCompleted is false, When getPlayedEpisodes is called, Then incomplete episodes are returned`() =
        runTest {
            // Given
            dao.upsertEpisodes(episodeEntities.take(3))
            dao.upsertPlayedEpisode(
                PlayedEpisodeEntity(
                    id = episodeEntities[0].id,
                    playedAt = Instant.fromEpochSeconds(0),
                    position = Duration.ZERO,
                    isCompleted = true,
                )
            )
            dao.upsertPlayedEpisode(
                PlayedEpisodeEntity(
                    id = episodeEntities[1].id,
                    playedAt = Instant.fromEpochSeconds(0),
                    position = Duration.ZERO,
                    isCompleted = false,
                )
            )

            // When
            dao.getPlayedEpisodes(isCompleted = false, limit = 10).test {
                val episodes = awaitItem()
                // Then
                assertEquals(1, episodes.size)
                assertEquals(false, episodes[0].isCompleted)
                cancel()
            }
        }

    @Test
    fun `Given query, When getPlayedEpisodes is called, Then filtered played episodes are returned`() =
        runTest {
            // Given
            dao.upsertEpisodes(
                listOf(
                    episodeEntity.copy(id = 100L, title = "Kotlin Programming"),
                    episodeEntity.copy(id = 101L, title = "Java Programming"),
                )
            )
            dao.upsertPlayedEpisode(
                PlayedEpisodeEntity(
                    id = 100L,
                    playedAt = Instant.fromEpochSeconds(0),
                    position = Duration.ZERO,
                    isCompleted = false,
                )
            )
            dao.upsertPlayedEpisode(
                PlayedEpisodeEntity(
                    id = 101L,
                    playedAt = Instant.fromEpochSeconds(0),
                    position = Duration.ZERO,
                    isCompleted = false,
                )
            )

            // When
            dao.getPlayedEpisodes(query = "Kotlin", limit = 10).test {
                val episodes = awaitItem()
                // Then
                assertEquals(1, episodes.size)
                assertEquals("Kotlin Programming", episodes[0].episode.title)
                cancel()
            }
        }

    @Test
    fun `Given query, When getPlayedEpisodesPaging is called, Then filtered played episodes are returned`() =
        runTest {
            // Given
            dao.upsertEpisodes(
                listOf(
                    episodeEntity.copy(id = 100L, title = "Kotlin Programming"),
                    episodeEntity.copy(id = 101L, title = "Java Programming"),
                )
            )
            dao.upsertPlayedEpisode(
                PlayedEpisodeEntity(
                    id = 100L,
                    playedAt = Instant.fromEpochSeconds(0),
                    position = Duration.ZERO,
                    isCompleted = false,
                )
            )
            dao.upsertPlayedEpisode(
                PlayedEpisodeEntity(
                    id = 101L,
                    playedAt = Instant.fromEpochSeconds(0),
                    position = Duration.ZERO,
                    isCompleted = false,
                )
            )

            // When
            val episodes = dao.getPlayedEpisodesPaging(query = "Kotlin").loadAsSnapshot()

            // Then
            assertEquals(1, episodes.size)
            assertEquals("Kotlin Programming", episodes[0].episode.title)
        }


    /** SAVED EPISODES **/

    @Test
    fun `Given episode, When addSavedEpisode is called, Then episode is saved`() =
        runTest {
            // Given
            dao.upsertEpisode(episodeEntity.copy(id = 100L))

            // When
            dao.addSavedEpisode(
                SavedEpisodeEntity(
                    id = 100L,
                    podcastId = episodeEntity.feedId,
                    savedAt = Instant.fromEpochSeconds(0),
                    filePath = "/data/episode_100.mp3",
                    totalSize = 1000L,
                    downloadedSize = 0L,
                    downloadStatus = DownloadStatus.PENDING,
                )
            )

            // Then
            dao.isSavedEpisode(100L).test {
                val isSaved = awaitItem()
                assertEquals(true, isSaved)
                cancel()
            }
        }

    @Test
    fun `Given saved episode, When removeSavedEpisode is called, Then episode is unsaved`() =
        runTest {
            // Given
            dao.upsertEpisode(episodeEntity.copy(id = 100L))
            dao.addSavedEpisode(
                SavedEpisodeEntity(
                    id = 100L,
                    podcastId = episodeEntity.feedId,
                    savedAt = Instant.fromEpochSeconds(0),
                    filePath = "/data/episode_100.mp3",
                    totalSize = 1000L,
                    downloadedSize = 0L,
                    downloadStatus = DownloadStatus.PENDING,
                )
            )

            // When
            dao.removeSavedEpisode(100L)

            // Then
            dao.isSavedEpisode(100L).test {
                val isSaved = awaitItem()
                assertEquals(false, isSaved)
                cancel()
            }
        }

    @Test
    fun `Given saved episode, When isSavedEpisode is called, Then true is returned`() =
        runTest {
            // Given
            dao.upsertEpisode(episodeEntity.copy(id = 100L))
            dao.addSavedEpisode(
                SavedEpisodeEntity(
                    id = 100L,
                    podcastId = episodeEntity.feedId,
                    savedAt = Instant.fromEpochSeconds(0),
                    filePath = "/data/episode_100.mp3",
                    totalSize = 1000L,
                    downloadedSize = 0L,
                    downloadStatus = DownloadStatus.PENDING,
                )
            )

            // When
            dao.isSavedEpisode(100L).test {
                val isSaved = awaitItem()
                // Then
                assertEquals(true, isSaved)
                cancel()
            }
        }

    @Test
    fun `Given unsaved episode, When isSavedEpisode is called, Then false is returned`() =
        runTest {
            // Given
            dao.upsertEpisode(episodeEntity.copy(id = 100L))

            // When
            dao.isSavedEpisode(100L).test {
                val isSaved = awaitItem()
                // Then
                assertEquals(false, isSaved)
                cancel()
            }
        }

    @Test
    fun `Given saved episode, When updateSavedEpisodeProgress is called, Then progress is updated`() =
        runTest {
            // Given
            dao.upsertEpisode(episodeEntity.copy(id = 100L))
            dao.addSavedEpisode(
                SavedEpisodeEntity(
                    id = 100L,
                    podcastId = episodeEntity.feedId,
                    savedAt = Instant.fromEpochSeconds(0),
                    filePath = "/data/episode_100.mp3",
                    totalSize = 1000L,
                    downloadedSize = 0L,
                    downloadStatus = DownloadStatus.PENDING,
                )
            )

            // When
            dao.updateSavedEpisodeProgress(100L, 500L, DownloadStatus.DOWNLOADING)

            // Then
            dao.getSavedEpisodes(limit = 10).test {
                val episodes = awaitItem()
                assertEquals(1, episodes.size)
                assertEquals(DownloadStatus.DOWNLOADING, episodes[0].downloadStatus)
                cancel()
            }
        }

    @Test
    fun `Given saved episode, When updateSavedEpisodeStatus is called, Then status is updated`() =
        runTest {
            // Given
            dao.upsertEpisode(episodeEntity.copy(id = 100L))
            dao.addSavedEpisode(
                SavedEpisodeEntity(
                    id = 100L,
                    podcastId = episodeEntity.feedId,
                    savedAt = Instant.fromEpochSeconds(0),
                    filePath = "/data/episode_100.mp3",
                    totalSize = 1000L,
                    downloadedSize = 0L,
                    downloadStatus = DownloadStatus.PENDING,
                )
            )

            // When
            dao.updateSavedEpisodeStatus(100L, DownloadStatus.COMPLETED)

            // Then
            dao.getSavedEpisodes(limit = 10).test {
                val episodes = awaitItem()
                assertEquals(1, episodes.size)
                assertEquals(DownloadStatus.COMPLETED, episodes[0].downloadStatus)
                cancel()
            }
        }

    @Test
    fun `Given unsaved episode, When toggleSavedEpisode is called, Then episode is saved`() =
        runTest {
            // Given
            val episode = episodeEntity.copy(id = 100L)
            dao.upsertEpisode(episode)

            // When
            val result = dao.toggleSavedEpisode(episode, "/data/episode_100.mp3")

            // Then
            assertEquals(true, result)
            dao.isSavedEpisode(100L).test {
                val isSaved = awaitItem()
                assertEquals(true, isSaved)
                cancel()
            }
        }

    @Test
    fun `Given saved episode, When toggleSavedEpisode is called, Then episode is unsaved`() =
        runTest {
            // Given
            val episode = episodeEntity.copy(id = 100L)
            dao.upsertEpisode(episode)
            dao.addSavedEpisode(
                SavedEpisodeEntity(
                    id = 100L,
                    podcastId = episode.feedId,
                    savedAt = Instant.fromEpochSeconds(0),
                    filePath = "/data/episode_100.mp3",
                    totalSize = 1000L,
                    downloadedSize = 0L,
                    downloadStatus = DownloadStatus.PENDING,
                )
            )

            // When
            val result = dao.toggleSavedEpisode(episode, "/data/episode_100.mp3")

            // Then
            assertEquals(false, result)
            dao.isSavedEpisode(100L).test {
                val isSaved = awaitItem()
                assertEquals(false, isSaved)
                cancel()
            }
        }

    @Test
    fun `Given no query, When getSavedEpisodes is called, Then all saved episodes are returned`() =
        runTest {
            // Given
            dao.upsertEpisodes(episodeEntities.take(3))
            episodeEntities.take(3).forEach {
                dao.addSavedEpisode(
                    SavedEpisodeEntity(
                        id = it.id,
                        podcastId = it.feedId,
                        savedAt = Instant.fromEpochSeconds(0),
                        filePath = "/data/episode_${it.id}.mp3",
                        totalSize = 1000L,
                        downloadedSize = 0L,
                        downloadStatus = DownloadStatus.PENDING,
                    )
                )
            }

            // When
            dao.getSavedEpisodes(limit = 10).test {
                val episodes = awaitItem()
                // Then
                assertEquals(3, episodes.size)
                cancel()
            }
        }

    @Test
    fun `Given query, When getSavedEpisodes is called, Then filtered saved episodes are returned`() =
        runTest {
            // Given
            dao.upsertEpisodes(
                listOf(
                    episodeEntity.copy(id = 100L, title = "Kotlin Programming"),
                    episodeEntity.copy(id = 101L, title = "Java Programming"),
                )
            )
            dao.addSavedEpisode(
                SavedEpisodeEntity(
                    id = 100L,
                    podcastId = episodeEntity.feedId,
                    savedAt = Instant.fromEpochSeconds(0),
                    filePath = "/data/episode_100.mp3",
                    totalSize = 1000L,
                    downloadedSize = 0L,
                    downloadStatus = DownloadStatus.PENDING,
                )
            )
            dao.addSavedEpisode(
                SavedEpisodeEntity(
                    id = 101L,
                    podcastId = episodeEntity.feedId,
                    savedAt = Instant.fromEpochSeconds(0),
                    filePath = "/data/episode_101.mp3",
                    totalSize = 1000L,
                    downloadedSize = 0L,
                    downloadStatus = DownloadStatus.PENDING,
                )
            )

            // When
            dao.getSavedEpisodes(query = "Kotlin", limit = 10).test {
                val episodes = awaitItem()
                // Then
                assertEquals(1, episodes.size)
                assertEquals("Kotlin Programming", episodes[0].episode.title)
                cancel()
            }
        }

    @Test
    fun `Given query, When getSavedEpisodesPaging is called, Then filtered saved episodes are returned`() =
        runTest {
            // Given
            dao.upsertEpisodes(
                listOf(
                    episodeEntity.copy(id = 100L, title = "Kotlin Programming"),
                    episodeEntity.copy(id = 101L, title = "Java Programming"),
                )
            )
            dao.addSavedEpisode(
                SavedEpisodeEntity(
                    id = 100L,
                    podcastId = episodeEntity.feedId,
                    savedAt = Instant.fromEpochSeconds(0),
                    filePath = "/data/episode_100.mp3",
                    totalSize = 1000L,
                    downloadedSize = 0L,
                    downloadStatus = DownloadStatus.PENDING,
                )
            )
            dao.addSavedEpisode(
                SavedEpisodeEntity(
                    id = 101L,
                    podcastId = episodeEntity.feedId,
                    savedAt = Instant.fromEpochSeconds(0),
                    filePath = "/data/episode_101.mp3",
                    totalSize = 1000L,
                    downloadedSize = 0L,
                    downloadStatus = DownloadStatus.PENDING,
                )
            )

            // When
            val episodes = dao.getSavedEpisodesPaging(query = "Kotlin").loadAsSnapshot()

            // Then
            assertEquals(1, episodes.size)
            assertEquals("Kotlin Programming", episodes[0].episode.title)
        }

    @Test
    fun `Given saved episode, When deleteEpisodesIfOrphaned is called, Then episode is not deleted`() =
        runTest {
            // Given
            dao.upsertEpisode(episodeEntity.copy(id = 100L))
            dao.addSavedEpisode(
                SavedEpisodeEntity(
                    id = 100L,
                    podcastId = episodeEntity.feedId,
                    savedAt = Instant.fromEpochSeconds(0),
                    filePath = "/data/episode_100.mp3",
                    totalSize = 1000L,
                    downloadedSize = 0L,
                    downloadStatus = DownloadStatus.PENDING,
                )
            )

            // When
            dao.deleteEpisodesIfOrphaned(listOf(100L))

            // Then
            dao.getEpisodes(limit = 10).test {
                val episodes = awaitItem()
                assertEquals(1, episodes.size)
                assertEquals(100L, episodes[0].episode.id)
                cancel()
            }
        }

    @Test
    fun `Given no saved episodes, When getSavedEpisodesPaging is called, Then empty result is returned`() =
        runTest {
            // Given
            dao.upsertEpisodes(episodeEntities.take(3))

            // When
            val episodes = dao.getSavedEpisodesPaging().loadAsSnapshot()

            // Then
            assertEquals(0, episodes.size)
        }

    @Test
    fun `Given episode ids once, When getEpisodesByIdsOnce is called, Then episodes are returned`() =
        runTest {
            // Given
            dao.upsertEpisodes(episodeEntities.take(3))
            val ids = episodeEntities.take(3).map { it.id }

            // When
            val episodes = dao.getEpisodesByIdsOnce(ids)

            // Then
            assertEquals(3, episodes.size)
            assertEquals(ids.toSet(), episodes.map { it.episode.id }.toSet())
        }

}