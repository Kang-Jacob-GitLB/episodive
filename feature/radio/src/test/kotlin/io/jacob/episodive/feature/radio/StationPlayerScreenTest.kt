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
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import androidx.test.core.app.ApplicationProvider
import io.jacob.episodive.core.designsystem.theme.EpisodiveTheme
import io.jacob.episodive.core.model.OnAirProgram
import io.jacob.episodive.core.model.StationDetails
import io.jacob.episodive.core.model.StationSchedule
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
        schedule: ScheduleOf? = null,
        uriHandler: UriHandler = FakeUriHandler(),
    ) {
        composeTestRule.setContent {
            CompositionLocalProvider(LocalUriHandler provides uriHandler) {
                EpisodiveTheme {
                    StationPlayerScreen(
                        state = state,
                        details = details,
                        onAir = onAir,
                        schedule = schedule,
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

    private val seoul = ZoneId.of("Asia/Seoul")
    private val hourMillis = 3_600_000L
    private val dayStart = LocalDateTime.of(2026, 10, 14, 0, 0).atZone(seoul).toInstant().toEpochMilli()

    /** 00시부터 한 시간씩 [count] 개. 29개면 내일 04시까지 이어진다. */
    private fun hourly(count: Int) = (0 until count).map { i ->
        onAirProgramTestData(
            title = "프로그램$i",
            startMillis = dayStart + i * hourMillis,
            endMillis = dayStart + (i + 1) * hourMillis,
            host = if (i % 2 == 0) "진행자$i" else null,
        )
    }

    private val programs = hourly(10)

    private fun scheduleOf(list: List<OnAirProgram> = programs) =
        ScheduleOf(activeState.station.id, StationSchedule(dayStart, list))

    private fun onAirOf(program: OnAirProgram?) = OnAirOf(activeState.station.id, program)

    private val scheduleTitle get() = context.getString(R.string.feature_radio_schedule_title)
    private val tomorrowLabel get() = context.getString(R.string.feature_radio_schedule_tomorrow)

    private fun row(text: String) = composeTestRule.onNodeWithText(text, substring = true, useUnmergedTree = true)

    private fun onAirRows() = composeTestRule.onAllNodes(
        SemanticsMatcher("방송 중 행") {
            it.config.getOrNull(SemanticsProperties.StateDescription) ==
                context.getString(R.string.feature_radio_schedule_on_air)
        },
        useUnmergedTree = true,
    )

    @Test
    fun `Given 편성표, Then 지금 방송 앞 1개와 뒤 4개만 보이고 진행자 없으면 제목만 보인다`() {
        setScreen(schedule = scheduleOf(), onAir = onAirOf(programs[5]))

        composeTestRule.onNodeWithText(scheduleTitle).performScrollTo().assertIsDisplayed()
        row("프로그램3").assertDoesNotExist()
        composeTestRule.onNodeWithText("프로그램4 · 진행자4", substring = true, useUnmergedTree = true).assertExists()
        composeTestRule.onNodeWithText("프로그램5", useUnmergedTree = true).assertExists()
        composeTestRule.onNodeWithText("프로그램9", useUnmergedTree = true).assertExists()
        composeTestRule.onNodeWithText("05:00", useUnmergedTree = true).assertExists()
    }

    @Test
    fun `Given 편성표, Then 지금 방송 행만 강조된다`() {
        setScreen(schedule = scheduleOf(), onAir = onAirOf(programs[5]))
        composeTestRule.onNodeWithText(scheduleTitle).performScrollTo()

        onAirRows().assertCountEquals(1)
        onAirRows()[0].assert(hasAnyDescendant(hasText("프로그램5", substring = true)))
    }

    @Test
    fun `Given 지금 방송이 다음 프로그램으로 넘어감, Then 강조와 창이 따라간다`() {
        var onAir by mutableStateOf<OnAirOf?>(onAirOf(programs[5]))
        composeTestRule.setContent {
            EpisodiveTheme { StationPlayerScreen(state = activeState, schedule = scheduleOf(), onAir = onAir) }
        }
        composeTestRule.onNodeWithText(scheduleTitle).performScrollTo()
        row("프로그램4").assertExists()

        // 곡만 다른 사본이어도 같은 칸으로 본다.
        onAir = onAirOf(programs[6].copy(song = "곡"))
        composeTestRule.waitForIdle()

        row("프로그램4").assertDoesNotExist()
        onAirRows()[0].assert(hasAnyDescendant(hasText("프로그램6", substring = true)))
    }

    @Test
    fun `Given 헤더를 누르면, Then 전부 펼쳐지고 다시 누르면 접힌다`() {
        setScreen(schedule = scheduleOf(), onAir = onAirOf(programs[5]))
        composeTestRule.onNodeWithText(scheduleTitle).performScrollTo().performClick()

        row("프로그램0").assertExists()
        row("프로그램9").assertExists()

        composeTestRule.onNodeWithText(scheduleTitle).performScrollTo().performClick()

        row("프로그램0").assertDoesNotExist()
    }

    @Test
    fun `Given 자정 가까이, Then 접힌 창이 내일로 이어지고 날짜가 바뀌는 행 앞에 내일 구분이 붙는다`() {
        val day = hourly(29)
        setScreen(schedule = scheduleOf(day), onAir = onAirOf(day[22]))
        composeTestRule.onNodeWithText(scheduleTitle).performScrollTo()

        // 이전 1 + 현재 + 다음 4 = 21..26. 내일(24시~)로 넘어간 행이 채워진다.
        row("프로그램21").assertExists()
        row("프로그램26").assertExists()
        row("프로그램27").assertDoesNotExist()
        row("프로그램20").assertDoesNotExist()
        composeTestRule.onAllNodesWithText(tomorrowLabel, useUnmergedTree = true).assertCountEquals(1)

        // 펼치면 내일 새벽 끝까지. 구분은 여전히 한 번이다.
        composeTestRule.onNodeWithText(scheduleTitle).performClick()
        row("프로그램28").assertExists()
        composeTestRule.onAllNodesWithText(tomorrowLabel, useUnmergedTree = true).assertCountEquals(1)
    }

    @Test
    fun `Given 오늘 안에서 끝나는 창, Then 내일 구분이 없다`() {
        setScreen(schedule = scheduleOf(), onAir = onAirOf(programs[5]))
        composeTestRule.onNodeWithText(scheduleTitle).performScrollTo()

        composeTestRule.onAllNodesWithText(tomorrowLabel, useUnmergedTree = true).assertCountEquals(0)
    }

    @Test
    fun `Given 어제 시작한 심야 프로그램이 첫 행, Then 내일 구분을 달지 않는다`() {
        val crossing = onAirProgramTestData("어제 심야", dayStart - hourMillis, dayStart + hourMillis)
        setScreen(schedule = scheduleOf(listOf(crossing) + programs.drop(1)), onAir = onAirOf(crossing))
        composeTestRule.onNodeWithText(scheduleTitle).performScrollTo()

        composeTestRule.onAllNodesWithText(tomorrowLabel, useUnmergedTree = true).assertCountEquals(0)
    }

    @Test
    fun `Given 지금 방송을 모름, Then 처음 6개만 보이고 강조는 없다`() {
        setScreen(schedule = scheduleOf(), onAir = null)
        composeTestRule.onNodeWithText(scheduleTitle).performScrollTo()

        composeTestRule.onNodeWithText("프로그램5", useUnmergedTree = true).assertExists()
        row("프로그램6").assertDoesNotExist()
        onAirRows().assertCountEquals(0)
    }

    @Test
    fun `Given 상세와 편성표, Then 편성표 카드가 홈페이지 링크와 태그 chip 사이에 있다`() {
        setScreen(
            details = shownDetails(stationDetailsTestData),
            schedule = scheduleOf(),
            onAir = onAirOf(programs[5]),
        )
        val title = composeTestRule.onNodeWithText(scheduleTitle)
        title.performScrollTo()

        val linkTop = composeTestRule.onNodeWithText("radio.example.com").getUnclippedBoundsInRoot().top
        val scheduleTop = title.getUnclippedBoundsInRoot().top
        val chipTop = composeTestRule.onNodeWithText("news").getUnclippedBoundsInRoot().top

        assertTrue(linkTop < scheduleTop)
        assertTrue(scheduleTop < chipTop)
    }

    @Test
    fun `Given 펼치기, Then 카드 위치가 링크와 chip 사이에 그대로다`() {
        setScreen(
            details = shownDetails(stationDetailsTestData),
            schedule = scheduleOf(),
            onAir = onAirOf(programs[5]),
        )
        composeTestRule.onNodeWithText(scheduleTitle).performScrollTo().performClick()
        composeTestRule.waitForIdle()

        val linkTop = composeTestRule.onNodeWithText("radio.example.com").getUnclippedBoundsInRoot().top
        val scheduleTop = composeTestRule.onNodeWithText(scheduleTitle).getUnclippedBoundsInRoot().top
        val chipTop = composeTestRule.onNodeWithText("news").getUnclippedBoundsInRoot().top

        assertTrue(linkTop < scheduleTop)
        assertTrue(scheduleTop < chipTop)
    }

    @Test
    fun `Given 편성표만 있고 상세 없음, Then 편성표만 보이고 출처 문구는 없다`() {
        setScreen(details = shownDetails(null), schedule = scheduleOf(), onAir = onAirOf(programs[5]))

        composeTestRule.onNodeWithText(scheduleTitle).performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText(context.getString(R.string.feature_radio_info_source)).assertDoesNotExist()
    }

    @Test
    fun `Given 편성표가 비었음, Then 섹션이 없다`() {
        setScreen(details = shownDetails(stationDetailsTestData), schedule = scheduleOf(emptyList()))
        composeTestRule.onNodeWithText(scheduleTitle).assertDoesNotExist()
        composeTestRule.onNodeWithText("radio.example.com").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun `Given 다른 스테이션의 편성표, Then 그리지 않는다`() {
        setScreen(
            schedule = ScheduleOf("other-station", StationSchedule(dayStart, programs)),
            onAir = onAirOf(programs[5]),
        )

        composeTestRule.onNodeWithText(scheduleTitle).assertDoesNotExist()
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
