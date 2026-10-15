package io.jacob.episodive.feature.radio

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import io.jacob.episodive.core.designsystem.theme.EpisodiveTheme
import io.jacob.episodive.core.model.OnAirProgram
import io.jacob.episodive.core.model.Station
import io.jacob.episodive.core.testing.model.bareOnAirProgramTestData
import io.jacob.episodive.core.testing.model.onAirProgramTestData
import io.jacob.episodive.core.testing.model.radioStationTestData
import io.jacob.episodive.core.ui.StationCoverImageTag
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class StationBarTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val context: Context by lazy { ApplicationProvider.getApplicationContext() }

    private fun setBar(
        streamTitle: String? = null,
        program: OnAirProgram? = null,
        station: Station = radioStationTestData,
        isPlaying: Boolean = true,
        isBuffering: Boolean = false,
        onStop: () -> Unit = {},
        onPlayOrPause: () -> Unit = {},
    ) {
        composeTestRule.setContent {
            EpisodiveTheme {
                StationBarContent(
                    state = StationBarState.Active(
                        station = station,
                        streamTitle = streamTitle,
                        program = program,
                        isPlaying = isPlaying,
                        isBuffering = isBuffering,
                    ),
                    onClick = {},
                    onPlayOrPause = onPlayOrPause,
                    onStop = onStop,
                )
            }
        }
    }

    // FadingEdgeText 는 양끝에 NBSP 를 덧붙이므로 부분 일치로 찾는다.
    @Test
    fun `Given 활성 상태, Then 이름과 LIVE 가 보인다`() {
        setBar()

        composeTestRule.onNodeWithText(radioStationTestData.name, substring = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("LIVE").assertIsDisplayed()
    }

    @Test
    fun `Given 빈 streamTitle, Then 방송사를 보여 준다`() {
        setBar(streamTitle = "  ")

        composeTestRule.onNodeWithText(radioStationTestData.broadcaster, substring = true).assertIsDisplayed()
    }

    @Test
    fun `Given 방송사가 빈 해외 스테이션, Then 부제로 지역을 보여 준다`() {
        setBar(station = radioStationTestData.copy(broadcaster = "", region = "California"))

        composeTestRule.onNodeWithText("California", substring = true).assertIsDisplayed()
    }

    @Test
    fun `Given 방송사도 지역도 빈 스테이션, Then 이름과 LIVE 만 보이고 크래시하지 않는다`() {
        setBar(station = radioStationTestData.copy(broadcaster = "", region = null))

        composeTestRule.onNodeWithText(radioStationTestData.name, substring = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("LIVE").assertIsDisplayed()
    }

    @Test
    fun `Given streamTitle, Then 제목을 보여 준다`() {
        setBar(streamTitle = "Artist - Title")

        composeTestRule.onNodeWithText("Artist - Title", substring = true).assertIsDisplayed()
    }

    @Test
    fun `Given 곡과 streamTitle 과 프로그램이 모두 있음, Then 곡이 먼저다`() {
        setBar(streamTitle = "Stream Title", program = onAirProgramTestData.copy(song = "곡 - 가수"))

        composeTestRule.onNodeWithText("곡 - 가수", substring = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("Stream Title", substring = true).assertDoesNotExist()
        composeTestRule.onNodeWithText(onAirProgramTestData.title, substring = true).assertDoesNotExist()
    }

    @Test
    fun `Given streamTitle 과 프로그램이 있고 곡은 없음, Then streamTitle 이 프로그램 제목보다 먼저다`() {
        setBar(streamTitle = "Stream Title", program = onAirProgramTestData)

        composeTestRule.onNodeWithText("Stream Title", substring = true).assertIsDisplayed()
        composeTestRule.onNodeWithText(onAirProgramTestData.title, substring = true).assertDoesNotExist()
    }

    @Test
    fun `Given 프로그램만 있음, Then 프로그램 제목을 보여 준다`() {
        setBar(program = onAirProgramTestData)

        composeTestRule.onNodeWithText(onAirProgramTestData.title, substring = true).assertIsDisplayed()
        composeTestRule.onNodeWithText(radioStationTestData.broadcaster, substring = true).assertDoesNotExist()
    }

    @Test
    fun `Given 프로그램 이미지 있음, Then 이미지를 요청하고 없으면 이니셜 아트만 있다`() {
        setBar(program = onAirProgramTestData)
        composeTestRule.onNodeWithTag(StationCoverImageTag, useUnmergedTree = true).assertExists()
    }

    @Test
    fun `Given 프로그램 이미지 없음, Then 이미지를 요청하지 않는다`() {
        setBar(program = bareOnAirProgramTestData)
        composeTestRule.onNodeWithTag(StationCoverImageTag, useUnmergedTree = true).assertDoesNotExist()
    }

    @Test
    fun `Given 버퍼링, Then Loading 이 있고 Play 는 없다`() {
        setBar(isPlaying = false, isBuffering = true)

        composeTestRule.onNodeWithContentDescription("Loading").assertExists()
        composeTestRule.onNodeWithContentDescription("Play").assertDoesNotExist()
    }

    @Test
    fun `Given 정지 클릭, Then 콜백이 불린다`() {
        var stopCount = 0
        setBar(onStop = { stopCount++ })

        composeTestRule
            .onNodeWithContentDescription(context.getString(R.string.feature_radio_stop))
            .performClick()

        assertEquals(1, stopCount)
    }
}
