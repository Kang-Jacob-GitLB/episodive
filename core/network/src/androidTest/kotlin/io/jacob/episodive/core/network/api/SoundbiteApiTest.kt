package io.jacob.episodive.core.network.api

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class SoundbiteApiTest {
    @get:Rule
    val retrofitRule = RetrofitRule()

    private lateinit var api: SoundbiteApi

    @Before
    fun setup() {
        api = retrofitRule.retrofit.create(SoundbiteApi::class.java)
    }

    @Test
    fun getSoundbitesTest() = runTest {
        val response = api.getSoundbites(
            max = 10,
        )
        val soundbite = response.dataList.first()

        assertNotNull(response)
        assertNotNull(soundbite)
        assertEquals(10, response.dataList.size)
    }
}