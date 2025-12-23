package io.jacob.episodive.core.network.datasource

import io.jacob.episodive.core.network.api.SoundbiteApi
import io.jacob.episodive.core.network.model.ResponseListWrapper
import io.jacob.episodive.core.network.model.SoundbiteResponse
import io.jacob.episodive.core.testing.util.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerifySequence
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

class SoundbiteRemoteDataSourceTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val soundbiteApi = mockk<SoundbiteApi>(relaxed = true)

    private val dataSource: SoundbiteRemoteDataSource = SoundbiteRemoteDataSourceImpl(
        soundbiteApi = soundbiteApi,
    )

    @Test
    fun `Given dependencies, When getSoundbites called, Then dao called`() =
        runTest {
            // Given
            val dataList = mockk<ResponseListWrapper<SoundbiteResponse>>(relaxed = true) {
                every { dataList } returns emptyList()
            }
            coEvery { soundbiteApi.getSoundbites(any()) } returns dataList

            // When
            dataSource.getSoundbites(5)

            // Then
            coVerifySequence {
                soundbiteApi.getSoundbites(any())
                dataList.dataList
            }
            confirmVerified(soundbiteApi)
        }
}