package io.jacob.episodive.core.database.datasource

import androidx.paging.PagingSource
import io.jacob.episodive.core.database.dao.FeedDao
import io.jacob.episodive.core.database.model.RecentFeedEntity
import io.jacob.episodive.core.database.model.RecentNewFeedEntity
import io.jacob.episodive.core.database.model.SoundbiteEntity
import io.jacob.episodive.core.database.model.TrendingFeedEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class FeedLocalDataSourceImpl @Inject constructor(
    private val feedDao: FeedDao,
) : FeedLocalDataSource {
    override suspend fun upsertTrendingFeeds(feeds: List<TrendingFeedEntity>) {
        feedDao.upsertTrendingFeeds(feeds)
    }

    override suspend fun upsertRecentFeeds(feeds: List<RecentFeedEntity>) {
        feedDao.upsertRecentFeeds(feeds)
    }

    override suspend fun upsertRecentNewFeeds(feeds: List<RecentNewFeedEntity>) {
        feedDao.upsertRecentNewFeeds(feeds)
    }

    override suspend fun upsertSoundbites(soundbites: List<SoundbiteEntity>) {
        feedDao.upsertSoundbites(soundbites)
    }

    override suspend fun deleteTrendingFeed(id: Long) {
        feedDao.deleteTrendingFeed(id)
    }

    override suspend fun deleteTrendingFeeds() {
        feedDao.deleteTrendingFeeds()
    }

    override suspend fun deleteTrendingFeedsByCacheKey(cacheKey: String) {
        feedDao.deleteTrendingFeedsByCacheKey(cacheKey)
    }

    override suspend fun deleteRecentFeed(id: Long) {
        feedDao.deleteRecentFeed(id)
    }

    override suspend fun deleteRecentFeeds() {
        feedDao.deleteRecentFeeds()
    }

    override suspend fun deleteRecentFeedsByCacheKey(cacheKey: String) {
        feedDao.deleteRecentFeedsByCacheKey(cacheKey)
    }

    override suspend fun deleteRecentNewFeed(id: Long) {
        feedDao.deleteRecentNewFeed(id)
    }

    override suspend fun deleteRecentNewFeeds() {
        feedDao.deleteRecentNewFeeds()
    }

    override suspend fun deleteRecentNewFeedsByCacheKey(cacheKey: String) {
        feedDao.deleteRecentNewFeedsByCacheKey(cacheKey)
    }

    override suspend fun deleteSoundbite(episodeId: Long) {
        feedDao.deleteSoundbite(episodeId)
    }

    override suspend fun deleteSoundbites() {
        feedDao.deleteSoundbites()
    }

    override suspend fun deleteSoundbitesByCacheKey(cacheKey: String) {
        feedDao.deleteSoundbitesByCacheKey(cacheKey)
    }

    override suspend fun replaceTrendingFeeds(feeds: List<TrendingFeedEntity>) {
        feedDao.replaceTrendingFeeds(feeds)
    }

    override suspend fun replaceRecentFeeds(feeds: List<RecentFeedEntity>) {
        feedDao.replaceRecentFeeds(feeds)
    }

    override suspend fun replaceRecentNewFeeds(feeds: List<RecentNewFeedEntity>) {
        feedDao.replaceRecentNewFeeds(feeds)
    }

    override suspend fun replaceSoundbites(soundbites: List<SoundbiteEntity>) {
        feedDao.replaceSoundbites(soundbites)
    }

    override fun getTrendingFeedsByCacheKey(
        cacheKey: String,
        limit: Int,
    ): Flow<List<TrendingFeedEntity>> {
        return feedDao.getTrendingFeedsByCacheKey(cacheKey, limit)
    }

    override fun getTrendingFeedsByCacheKeyPaging(cacheKey: String): PagingSource<Int, TrendingFeedEntity> {
        return feedDao.getTrendingFeedsByCacheKeyPaging(cacheKey)
    }

    override fun getRecentFeedsByCacheKey(
        cacheKey: String,
        limit: Int,
    ): Flow<List<RecentFeedEntity>> {
        return feedDao.getRecentFeedsByCacheKey(cacheKey, limit)
    }

    override fun getRecentFeedsByCacheKeyPaging(cacheKey: String): PagingSource<Int, RecentFeedEntity> {
        return feedDao.getRecentFeedsByCacheKeyPaging(cacheKey)
    }

    override fun getRecentNewFeedsByCacheKey(
        cacheKey: String,
        limit: Int,
    ): Flow<List<RecentNewFeedEntity>> {
        return feedDao.getRecentNewFeedsByCacheKey(cacheKey, limit)
    }

    override fun getRecentNewFeedsByCacheKeyPaging(cacheKey: String): PagingSource<Int, RecentNewFeedEntity> {
        return feedDao.getRecentNewFeedsByCacheKeyPaging(cacheKey)
    }

    override fun getSoundbitesByCacheKey(
        cacheKey: String,
        limit: Int,
    ): Flow<List<SoundbiteEntity>> {
        return feedDao.getSoundbitesByCacheKey(cacheKey, limit)
    }

    override fun getSoundbitesByCacheKeyPaging(cacheKey: String): PagingSource<Int, SoundbiteEntity> {
        return feedDao.getSoundbitesByCacheKeyPaging(cacheKey)
    }
}