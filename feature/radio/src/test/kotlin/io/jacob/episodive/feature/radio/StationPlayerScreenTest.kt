package io.jacob.episodive.feature.radio

import android.content.Context
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import androidx.test.core.app.ApplicationProvider
import io.jacob.episodive.core.designsystem.theme.EpisodiveTheme
import io.jacob.episodive.core.model.StationDetails
import io.jacob.episodive.core.testing.model.bareOnAirProgramTestData
import io.jacob.episodive.core.testing.model.codecOnlyStationDetailsTestData
import io.jacob.episodive.core.testing.model.kbsFirstRadioStationTestData
import io.jacob.episodive.core.testing.model.onAirProgramTestData
import io.jacob.episodive.core.testing.model.radioStationTestData
import io.jacob.episodive.core.testing.model.regionalRadioStationTestData
import io.jacob.episodive.core.testing.model.stationDetailsTestData
import io.jacob.episodive.core.ui.StationCoverImageTag
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.LocalDateTime
import java.time.ZoneId
import io.jacob.episodive.core.ui.R as uiR

@RunWith(RobolectricTestRunner::class)
class StationPlayerScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val context: Context by lazy { ApplicationProvider.getApplicationContext() }

    private val activeState = StationPlayerState.Active(
        station = regionalRadioStationTestData,
        isPending = false,
        streamTitle = null,
        isPlaying = true,
        isBuffering = false,
        isFavorite = false,
        previous = radioStationTestData,
        next = kbsFirstRadioStationTestData,
        sleepTimerRemainingMs = null,
    )

    /** activeState 가 보이는 스테이션의 상세. */
    private fun shownDetails(details: StationDetails?) = StationDetailsOf(activeState.station.id, details)

    private fun setScreen(
        state: StationPlayerState.Active = activeState,
        onPlayOrPause: () -> Unit = {},
        onStop: () -> Unit = {},
        onToggleFavorite: () -> Unit = {},
        details: StationDetailsOf? = null,
        onAir: OnAirOf? = null,
        uriHandler: UriHandler = FakeUriHandler(),
    ) {
        composeTestRule.setContent {
            CompositionLocalProvider(LocalUriHandler provides uriHandler) {
                EpisodiveTheme {
                    StationPlayerScreen(
                        state = state,
                        details = details,
                        onAir = onAir,
                        onPlayOrPause = onPlayOrPause,
                        onStop = onStop,
                        onToggleFavorite = onToggleFavorite,
                    )
                }
            }
        }
    }

    private val previousDescription get() = context.getString(R.string.feature_radio_previous)
    private val nextDescription get() = context.getString(R.string.feature_radio_next)

    @Test
    fun `Given 활성 상태, Then 이름과 방송사 지역과 LIVE 배지와 지금 방송이 보인다`() {
        setScreen()

        composeTestRule.onNodeWithText(regionalRadioStationTestData.name, substring = true).assertIsDisplayed()
        composeTestRule.onNodeWithText(
            "${regionalRadioStationTestData.broadcaster} · ${regionalRadioStationTestData.region}",
            substring = true,
        ).assertIsDisplayed()
        composeTestRule.onNodeWithText(context.getString(uiR.string.core_ui_station_live)).assertIsDisplayed()
        composeTestRule.onNodeWithText(context.getString(uiR.string.core_ui_live_now)).assertIsDisplayed()
    }

    private val program = onAirProgramTestData.copy(
        startMillis = LocalDateTime.of(2026, 10, 14, 9, 0).atZone(ZoneId.of("Asia/Seoul")).toInstant().toEpochMilli(),
        endMillis = LocalDateTime.of(2026, 10, 14, 11, 0).atZone(ZoneId.of("Asia/Seoul")).toInstant().toEpochMilli(),
    )

    @Test
    fun `Given 지금 스테이션의 프로그램, Then 제목과 진행자와 방송 시간이 보이고 이미지를 요청한다`() {
        setScreen(onAir = OnAirOf(activeState.station.id, program))

        composeTestRule.onNodeWithText("${program.title} · ${program.host}", substring = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("09:00 – 11:00").assertIsDisplayed()
        composeTestRule.onNodeWithTag(StationCoverImageTag, useUnmergedTree = true).assertExists()
    }

    @Test
    fun `Given 다른 스테이션의 프로그램, Then 그리지 않는다`() {
        setScreen(onAir = OnAirOf("other-station", program))

        composeTestRule.onNodeWithText(program.title, substring = true).assertDoesNotExist()
        composeTestRule.onNodeWithText("09:00 – 11:00").assertDoesNotExist()
        composeTestRule.onNodeWithTag(StationCoverImageTag, useUnmergedTree = true).assertDoesNotExist()
    }

    @Test
    fun `Given 이미지 없는 프로그램, Then 이니셜 아트만 있고 제목은 보인다`() {
        setScreen(onAir = OnAirOf(activeState.station.id, bareOnAirProgramTestData))

        composeTestRule.onNodeWithText(bareOnAirProgramTestData.title, substring = true).assertIsDisplayed()
        composeTestRule.onNodeWithTag(StationCoverImageTag, useUnmergedTree = true).assertDoesNotExist()
    }

    @Test
    fun `Given 진행자 없음, Then 제목만 보인다`() {
        setScreen(onAir = OnAirOf(activeState.station.id, bareOnAirProgramTestData))

        composeTestRule.onNodeWithText("${bareOnAirProgramTestData.title} ·", substring = true).assertDoesNotExist()
    }

    @Test
    fun `Given 곡과 streamTitle, Then 곡이 먼저이고 곡이 없으면 streamTitle 이다`() {
        var onAir by mutableStateOf<OnAirOf?>(OnAirOf(activeState.station.id, program.copy(song = "곡 - 가수")))
        composeTestRule.setContent {
            EpisodiveTheme {
                StationPlayerScreen(state = activeState.copy(streamTitle = "Stream Title"), onAir = onAir)
            }
        }
        composeTestRule.onNodeWithText("곡 - 가수", substring = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("Stream Title", substring = true).assertDoesNotExist()

        onAir = OnAirOf(activeState.station.id, program)
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Stream Title", substring = true).assertIsDisplayed()
    }

    @Test
    fun `Given 프로그램 유무, Then 이전 버튼 위치가 같다`() {
        var onAir by mutableStateOf<OnAirOf?>(null)
        composeTestRule.setContent {
            EpisodiveTheme { StationPlayerScreen(state = activeState, onAir = onAir) }
        }
        val without = composeTestRule.onNodeWithContentDescription(previousDescription).getUnclippedBoundsInRoot()

        onAir = OnAirOf(activeState.station.id, program)
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText(program.title, substring = true).assertIsDisplayed()
        val with = composeTestRule.onNodeWithContentDescription(previousDescription).getUnclippedBoundsInRoot()

        assertEquals(without, with)
    }

    @Test
    fun `Given 활성 상태, Then 탐색 버튼과 시간 표시가 없다`() {
        setScreen()

        composeTestRule.onNodeWithContentDescription("Replay").assertDoesNotExist()
        composeTestRule.onNodeWithContentDescription("Forward").assertDoesNotExist()
        composeTestRule.onAllNodes(
            SemanticsMatcher("시간 표시") { node ->
                node.config.getOrNull(SemanticsProperties.Text)
                    ?.any { Regex("""\d+:\d\d""").containsMatchIn(it.text) } == true
            }
        ).assertCountEquals(0)
    }

    @Test
    fun `Given 곡명이 있다 없다 해도, Then 이전 버튼 위치가 같다`() {
        var state by mutableStateOf(activeState)
        composeTestRule.setContent {
            EpisodiveTheme { StationPlayerScreen(state = state) }
        }
        val without = composeTestRule.onNodeWithContentDescription(previousDescription).getUnclippedBoundsInRoot()

        state = activeState.copy(streamTitle = "Artist - Title")
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Artist - Title", substring = true).assertIsDisplayed()
        val with = composeTestRule.onNodeWithContentDescription(previousDescription).getUnclippedBoundsInRoot()

        assertEquals(without, with)
    }

    @Test
    fun `Given 받는 중, Then 스피너가 있고 Play 는 없다`() {
        setScreen(activeState.copy(isPending = true, isPlaying = false, isBuffering = false))

        composeTestRule.onNodeWithContentDescription("Loading").assertExists()
    }

    @Test
    fun `Given 일시정지 버퍼링, Then 스피너가 있다`() {
        setScreen(activeState.copy(isPlaying = false, isBuffering = true))

        composeTestRule.onNodeWithContentDescription("Loading").assertExists()
    }

    @Test
    fun `Given 재생 중, Then 스피너가 없다`() {
        setScreen()

        composeTestRule.onNodeWithContentDescription("Loading").assertDoesNotExist()
    }

    @Test
    fun `Given 이웃이 없음, Then 이전 다음이 비활성이다`() {
        setScreen(activeState.copy(previous = null, next = null))

        composeTestRule.onNodeWithContentDescription(previousDescription).assertIsNotEnabled()
        composeTestRule.onNodeWithContentDescription(nextDescription).assertIsNotEnabled()
    }

    @Test
    fun `Given 이웃이 있음, Then 이전 다음이 활성이다`() {
        setScreen()

        composeTestRule.onNodeWithContentDescription(previousDescription).assertIsEnabled()
        composeTestRule.onNodeWithContentDescription(nextDescription).assertIsEnabled()
    }

    @Test
    fun `Given 정지 클릭, Then 콜백이 불린다`() {
        var stopCount = 0
        setScreen(onStop = { stopCount++ })

        composeTestRule
            .onNodeWithContentDescription(context.getString(R.string.feature_radio_stop))
            .performClick()

        assertEquals(1, stopCount)
    }

    @Test
    fun `Given 즐겨찾기 클릭, Then 콜백이 불린다`() {
        var toggleCount = 0
        setScreen(onToggleFavorite = { toggleCount++ })

        composeTestRule
            .onNodeWithContentDescription(context.getString(uiR.string.core_ui_station_favorite_add))
            .performClick()

        assertEquals(1, toggleCount)
    }

    @Test
    fun `Given 타이머 시트를 열면, Then 에피소드 끝까지 항목이 없다`() {
        setScreen()

        composeTestRule
            .onNodeWithContentDescription(context.getString(uiR.string.core_ui_sleep_timer))
            .performClick()

        composeTestRule.onNodeWithText("5m").assertIsDisplayed()
        composeTestRule
            .onNodeWithText(context.getString(uiR.string.core_ui_sleep_timer_end_of_episode))
            .assertDoesNotExist()
    }

    @Test
    fun `Given 상세 없음, Then 정보 섹션이 없다`() {
        setScreen(details = null)

        composeTestRule.onNodeWithText(context.getString(R.string.feature_radio_info_source)).assertDoesNotExist()
    }

    @Test
    fun `Given 상세 있음, Then 홈페이지 음질 태그 출처가 보인다`() {
        setScreen(details = shownDetails(stationDetailsTestData))

        composeTestRule.onNodeWithText("radio.example.com").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText(
            "AAC · ${context.getString(R.string.feature_radio_info_bitrate_format, 128)}",
        ).performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("news").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("talk").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText(context.getString(R.string.feature_radio_info_source))
            .performScrollTo().assertIsDisplayed()
    }

    @Test
    fun `Given 음질만 있는 상세, Then 홈페이지와 태그 줄은 없다`() {
        setScreen(details = shownDetails(codecOnlyStationDetailsTestData))

        composeTestRule.onNodeWithText("MP3").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("radio.example.com").assertDoesNotExist()
        composeTestRule.onNodeWithText("news").assertDoesNotExist()
    }

    @Test
    fun `When 홈페이지 행을 누르면, Then 원래 주소로 연다`() {
        val opened = mutableListOf<String>()
        setScreen(details = shownDetails(stationDetailsTestData), uriHandler = FakeUriHandler { opened += it })

        composeTestRule.onNodeWithText("radio.example.com").performScrollTo().performClick()

        assertEquals(listOf("https://radio.example.com"), opened)
    }

    @Test
    fun `Given 열 앱이 없어 던지는 핸들러, When 홈페이지 행을 누르면, Then 크래시하지 않는다`() {
        setScreen(
            details = shownDetails(stationDetailsTestData),
            uriHandler = FakeUriHandler { throw IllegalStateException("no browser") },
        )

        composeTestRule.onNodeWithText("radio.example.com").performScrollTo().performClick()
    }

    @Test
    fun `Given 다른 스테이션의 상세, Then 정보 섹션을 그리지 않는다`() {
        setScreen(details = StationDetailsOf(kbsFirstRadioStationTestData.id, stationDetailsTestData))

        composeTestRule.onNodeWithText("radio.example.com").assertDoesNotExist()
        composeTestRule.onNodeWithText(context.getString(R.string.feature_radio_info_source)).assertDoesNotExist()
    }

    @Test
    fun `Given 상세 있음, Then 홈페이지 행의 터치 영역은 48dp 이상이다`() {
        setScreen(details = shownDetails(stationDetailsTestData))

        composeTestRule.onNode(hasClickAction() and hasText("radio.example.com"))
            .performScrollTo()
            .getUnclippedBoundsInRoot()
            .let { assertTrue(it.height >= 48.dp) }
    }
}

private class FakeUriHandler(private val onOpen: (String) -> Unit = {}) : UriHandler {
    override fun openUri(uri: String) = onOpen(uri)
}
