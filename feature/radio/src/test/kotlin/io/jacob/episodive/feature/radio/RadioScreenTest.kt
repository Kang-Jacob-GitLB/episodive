package io.jacob.episodive.feature.radio

import android.content.Context
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithText
import androidx.test.core.app.ApplicationProvider
import io.jacob.episodive.core.designsystem.theme.EpisodiveTheme
import io.jacob.episodive.core.model.Station
import io.jacob.episodive.core.testing.model.radioStationTestData
import io.jacob.episodive.core.testing.model.radioStationsTestData
import io.jacob.episodive.core.ui.StationCoverImageTag
import io.jacob.episodive.core.ui.R as uiR
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class RadioScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val context: Context by lazy { ApplicationProvider.getApplicationContext() }

    private val a = radioStationTestData

    private fun setScreen(
        favorites: List<Station> = emptyList(),
        recents: List<Station> = emptyList(),
        nowPlayingStationId: String? = null,
        isPlaying: Boolean = false,
        pendingStationId: String? = null,
        programImages: Map<String, String> = emptyMap(),
    ) {
        composeTestRule.setContent {
            EpisodiveTheme {
                RadioScreen(
                    state = RadioState.Success(
                        favorites = favorites,
                        recents = recents,
                        nationwide = radioStationsTestData.filter { it.region == null },
                        regional = radioStationsTestData.filter { it.region != null },
                        favoriteIds = favorites.map { it.id }.toSet(),
                        nowPlayingStationId = nowPlayingStationId,
                        isPlaying = isPlaying,
                        isBuffering = false,
                        pendingStationId = pendingStationId,
                    ),
                    programImages = programImages,
                    onBackClick = {},
                    onStationClick = {},
                    onToggleFavorite = {},
                    onRetry = {},
                )
            }
        }
    }

    @Test
    fun `Given 즐겨찾기와 최근이 비어 있음, Then 두 섹션 제목이 없다`() {
        setScreen()

        composeTestRule
            .onAllNodesWithText(context.getString(R.string.feature_radio_section_favorites))
            .assertCountEquals(0)
        composeTestRule
            .onAllNodesWithText(context.getString(R.string.feature_radio_section_recent))
            .assertCountEquals(0)
        composeTestRule
            .onNodeWithText(context.getString(R.string.feature_radio_section_nationwide))
            .assertExists()
    }

    @Test
    fun `Given 같은 스테이션이 세 섹션에 나옴, Then 크래시 없이 그려진다`() {
        setScreen(favorites = listOf(a), recents = listOf(a))

        composeTestRule
            .onNodeWithText(context.getString(R.string.feature_radio_section_favorites))
            .assertExists()
        composeTestRule
            .onNodeWithText(context.getString(R.string.feature_radio_section_recent))
            .assertExists()
    }

    @Test
    fun `Given 재생 중, Then 카드 상태 설명이 재생 중이다`() {
        setScreen(favorites = listOf(a), nowPlayingStationId = a.id, isPlaying = true)

        val nowPlaying = context.getString(uiR.string.core_ui_now_playing)
        composeTestRule
            .onAllNodes(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, nowPlaying))
            .assertCountEquals(2)
    }

    @Test
    fun `Given 주소를 받는 중, Then Loading 이 있다`() {
        setScreen(favorites = listOf(a), pendingStationId = a.id)

        composeTestRule.onAllNodesWithContentDescription("Loading").assertCountEquals(2)
    }

    @Test
    fun `Given 즐겨찾기 상태, Then 별 토글의 설명이 상태별로 다르다`() {
        setScreen(favorites = listOf(a))

        composeTestRule
            .onAllNodesWithContentDescription(context.getString(uiR.string.core_ui_station_favorite_remove))
            .assertCountEquals(1)
        composeTestRule
            .onAllNodesWithContentDescription(context.getString(uiR.string.core_ui_station_favorite_add))
            .onFirst()
            .assertExists()
    }

    @Test
    fun `Given 프로그램 이미지가 있는 스테이션만, Then 그 행만 이미지를 요청하고 나머지는 이니셜 아트다`() {
        setScreen(programImages = mapOf(a.id to "https://img.example.com/a.jpg"))

        composeTestRule.onAllNodes(hasTestTag(StationCoverImageTag), useUnmergedTree = true).assertCountEquals(1)
    }

    @Test
    fun `Given 프로그램 이미지가 없음, Then 이미지를 요청하지 않는다`() {
        setScreen()

        composeTestRule.onAllNodes(hasTestTag(StationCoverImageTag), useUnmergedTree = true).assertCountEquals(0)
    }

    @Test
    fun `Given 즐겨찾기에도 있는 스테이션의 이미지, Then 캐러셀과 목록 모두에 쓰인다`() {
        setScreen(favorites = listOf(a), programImages = mapOf(a.id to "https://img.example.com/a.jpg"))

        composeTestRule.onAllNodes(hasTestTag(StationCoverImageTag), useUnmergedTree = true).assertCountEquals(2)
    }
}
