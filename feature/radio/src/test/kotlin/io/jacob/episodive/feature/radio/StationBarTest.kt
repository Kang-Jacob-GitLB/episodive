package io.jacob.episodive.feature.radio

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import io.jacob.episodive.core.designsystem.theme.EpisodiveTheme
import io.jacob.episodive.core.testing.model.radioStationTestData
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
        isPlaying: Boolean = true,
        isBuffering: Boolean = false,
        onStop: () -> Unit = {},
        onPlayOrPause: () -> Unit = {},
    ) {
        composeTestRule.setContent {
            EpisodiveTheme {
                StationBarContent(
                    state = StationBarState.Active(
                        station = radioStationTestData,
                        streamTitle = streamTitle,
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
    fun `Given streamTitle, Then 제목을 보여 준다`() {
        setBar(streamTitle = "Artist - Title")

        composeTestRule.onNodeWithText("Artist - Title", substring = true).assertIsDisplayed()
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
