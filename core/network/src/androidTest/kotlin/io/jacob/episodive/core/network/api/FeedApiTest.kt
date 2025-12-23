package io.jacob.episodive.core.network.api

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class FeedApiTest {
    @get:Rule
    val retrofitRule = RetrofitRule()

    private lateinit var api: FeedApi

    @Before
    fun setup() {
        api = retrofitRule.retrofit.create(FeedApi::class.java)
    }

    @Test
    fun getTrendingFeedsTest() = runTest {
        val response = api.getTrendingFeeds(
            max = 10
        )

        assertNotNull(response)
        assertEquals(10, response.dataList.size)
    }

    @Test
    fun getRecentFeedsTest() = runTest {
        val response = api.getRecentFeeds(
            max = 10,
        )
        val feed = response.dataList.first()

        assertNotNull(response)
        assertNotNull(feed)
        assertEquals(10, response.dataList.size)
    }

    @Test
    fun getRecentNewFeedsTest() = runTest {
        val response = api.getRecentNewFeeds(
            max = 10,
        )
        val feed = response.dataList.first()

        assertNotNull(response)
        assertNotNull(feed)
        assertEquals(10, response.dataList.size)
    }

    @Test
    fun getRecentNewValueFeedsTest() = runTest {
        val response = api.getRecentNewValueFeeds(
            max = 10,
        )
        val feed = response.dataList.first()

        assertNotNull(response)
        assertNotNull(feed)
        assertEquals(10, response.dataList.size)
    }
}