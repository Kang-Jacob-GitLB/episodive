package io.jacob.episodive.core.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import io.jacob.episodive.core.designsystem.theme.EpisodiveTheme
import io.jacob.episodive.core.model.Station
import io.jacob.episodive.core.testing.model.radioStationTestData
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class StationsSectionTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun station(index: Int): Station =
        radioStationTestData.copy(id = "s$index", name = "방송$index")

    @Test
    fun `Given 첫 항목이 바뀜, When 다른 곳으로 스크롤돼 있어도, Then 새 첫 카드가 화면에 있다`() {
        var stations by mutableStateOf((0 until 12).map(::station))
        composeTestRule.setContent {
            EpisodiveTheme {
                StationsSection(
                    title = "Recent",
                    stations = stations,
                    statusOf = { StationCardStatus.Idle },
                    onStationClick = {},
                )
            }
        }
        composeTestRule.onNodeWithText("방송0").assertIsDisplayed()

        // 방송9 를 튼 것처럼 맨 앞으로 옮긴다.
        stations = listOf(station(9)) + stations.filter { it.id != "s9" }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("방송9").assertIsDisplayed()
    }
}
