package io.jacob.episodive.feature.player

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.text.TextLayoutResult
import io.jacob.episodive.core.designsystem.theme.EpisodiveTheme
import io.jacob.episodive.core.model.Chapter
import io.jacob.episodive.core.model.Episode
import io.jacob.episodive.core.model.Podcast
import io.jacob.episodive.core.model.Progress
import io.jacob.episodive.core.model.caption.LiveCaption
import io.jacob.episodive.core.testing.model.captionLineOf
import io.jacob.episodive.core.testing.model.captionLineTestData
import io.jacob.episodive.core.testing.model.episodeTestData
import io.jacob.episodive.core.testing.model.episodeTestDataList
import io.jacob.episodive.core.testing.model.liveCaptionTestData
import io.jacob.episodive.core.testing.model.podcastTestData
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PlayerScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val defaultChapters = listOf(
        Chapter("Chapter 1", 0.seconds, 1000.seconds),
        Chapter("Chapter 2", 1000.seconds, 3000.seconds),
        Chapter("Chapter 3", 3000.seconds, 6000.seconds),
    )

    private fun setPlayerScreen(
        podcast: Podcast = podcastTestData,
        nowPlaying: Episode = episodeTestData,
        progress: Progress = Progress(1000.seconds, 2000.seconds, 6000.seconds),
        isPlaying: Boolean = true,
        chapters: List<Chapter> = defaultChapters,
        playlist: List<Episode> = episodeTestDataList,
        indexOfList: Int = 0,
        speed: Float = 1f,
        cue: String = "test cue text",
        onCollapse: () -> Unit = {},
        onToggleLike: () -> Unit = {},
        onToggleSave: () -> Unit = {},
        onSeekTo: (Long) -> Unit = {},
        onPlayOrPause: () -> Unit = {},
        onBackward: () -> Unit = {},
        onForward: () -> Unit = {},
        onPrevious: () -> Unit = {},
        onNext: () -> Unit = {},
        onPodcastClick: (Podcast) -> Unit = {},
        onEpisodeClick: (Episode) -> Unit = {},
        onPlayIndex: (Int) -> Unit = {},
        onToggleLikedEpisode: (Episode) -> Unit = {},
        onToggleSavedEpisode: (Episode) -> Unit = {},
        onSpeedChange: (Float) -> Unit = {},
        onToggleFollowedPodcast: (Podcast) -> Unit = {},
        onShare: () -> Unit = {},
        liveCaption: () -> LiveCaption? = { null },
        captionButtonState: () -> CaptionButtonState = { CaptionButtonState.Inactive },
        onToggleCaption: () -> Unit = {},
    ) {
        composeTestRule.setContent {
            EpisodiveTheme {
                PlayerScreen(
                    podcast = podcast,
                    nowPlaying = nowPlaying,
                    progress = progress,
                    isPlaying = isPlaying,
                    onCollapse = onCollapse,
                    onToggleLike = onToggleLike,
                    onShare = onShare,
                    onSeekTo = onSeekTo,
                    onPlayOrPause = onPlayOrPause,
                    onBackward = onBackward,
                    onForward = onForward,
                    onPrevious = onPrevious,
                    onNext = onNext,
                    onPodcastClick = onPodcastClick,
                    playlist = playlist,
                    indexOfList = indexOfList,
                    onEpisodeClick = onEpisodeClick,
                    onPlayIndex = onPlayIndex,
                    onToggleLikedEpisode = onToggleLikedEpisode,
                    onToggleSavedEpisode = onToggleSavedEpisode,
                    speed = speed,
                    onSpeedChange = onSpeedChange,
                    chapters = chapters,
                    onToggleFollowedPodcast = onToggleFollowedPodcast,
                    onToggleSave = onToggleSave,
                    cue = cue,
                    liveCaption = liveCaption,
                    captionButtonState = captionButtonState,
                    onToggleCaption = onToggleCaption,
                )
            }
        }
    }

    // --- Basic display tests ---

    @Test
    fun episodeTitleAndPodcastTitleAreDisplayed() {
        setPlayerScreen()

        composeTestRule.onNodeWithText(episodeTestData.title, substring = true).assertIsDisplayed()
        composeTestRule.onNodeWithText(podcastTestData.title, substring = true).assertIsDisplayed()
    }

    @Test
    fun playbackControlsAreDisplayed() {
        setPlayerScreen()

        composeTestRule.onNodeWithText("1x").assertExists()
    }

    @Test
    fun speedIsDisplayed() {
        setPlayerScreen()

        composeTestRule.onNodeWithText("1x").assertExists()
    }

    @Test
    fun episodeInfoSectionIsDisplayed() {
        setPlayerScreen()

        composeTestRule.onNodeWithText("Episode Info").assertIsDisplayed()
    }

    @Test
    fun podcastTitleInPlayerIsDisplayed() {
        setPlayerScreen()

        composeTestRule.onNodeWithText(podcastTestData.title, substring = true).assertIsDisplayed()
    }

    @Test
    fun nowPlayingTitleInPlayerIsDisplayed() {
        setPlayerScreen()

        composeTestRule.onNodeWithText(episodeTestData.title, substring = true).assertIsDisplayed()
    }

    @Test
    fun replayButtonIsDisplayed() {
        setPlayerScreen()

        composeTestRule.onNodeWithContentDescription("Replay").assertExists()
    }

    @Test
    fun noChapters_chapterSectionIsNotShown() {
        setPlayerScreen(chapters = emptyList(), cue = "")

        composeTestRule.onNodeWithText("Chapter").assertDoesNotExist()
    }

    @Test
    fun cueTextIsDisplayed() {
        setPlayerScreen()

        composeTestRule.onNodeWithText("test cue text").assertExists()
    }

    @Test
    fun progressTimesAreDisplayed() {
        setPlayerScreen()

        composeTestRule.onNodeWithText("16:40", substring = true).assertExists()
    }

    @Test
    fun forwardButtonIsDisplayed() {
        setPlayerScreen()

        composeTestRule.onNodeWithContentDescription("Forward").assertExists()
    }

    @Test
    fun previousButtonIsDisplayed() {
        setPlayerScreen()

        composeTestRule.onNodeWithContentDescription("Previous").assertExists()
    }

    @Test
    fun nextButtonIsDisplayed() {
        setPlayerScreen()

        composeTestRule.onNodeWithContentDescription("Next").assertExists()
    }

    @Test
    fun whenPlaying_pauseButtonIsDisplayed() {
        setPlayerScreen(isPlaying = true)

        composeTestRule.onNodeWithContentDescription("Pause").assertExists()
    }

    @Test
    fun whenNotPlaying_playButtonIsDisplayed() {
        setPlayerScreen(isPlaying = false, chapters = emptyList(), cue = "")

        composeTestRule.onNodeWithContentDescription("Play").assertExists()
    }

    @Test
    fun shuffleButtonIsDisplayed() {
        setPlayerScreen()

        composeTestRule.onNodeWithContentDescription("List").assertExists()
    }

    // --- New: Chapter section tests ---

    @Test
    fun withChapters_chapterSectionIsShown() {
        setPlayerScreen(chapters = defaultChapters)

        composeTestRule.onNode(hasScrollAction())
            .performScrollToNode(hasText("Chapter"))
        composeTestRule.onNodeWithText("Chapter").assertExists()
    }

    @Test
    fun withChapters_chapterNamesAreDisplayed() {
        setPlayerScreen(chapters = defaultChapters)

        composeTestRule.onNode(hasScrollAction())
            .performScrollToNode(hasText("Chapter 1"))
        composeTestRule.onNodeWithText("Chapter 1").assertExists()
    }

    // --- New: Podcast Info section ---

    @Test
    fun podcastInfoSectionIsDisplayed() {
        setPlayerScreen()

        composeTestRule.onNode(hasScrollAction())
            .performScrollToNode(hasText("Podcast Info"))
        composeTestRule.onNodeWithText("Podcast Info").assertExists()
    }

    // --- New: Empty cue ---

    @Test
    fun emptyCue_noCueTextShown() {
        setPlayerScreen(cue = "")

        composeTestRule.onNodeWithText("test cue text").assertDoesNotExist()
    }

    // --- New: Different speed values ---

    @Test
    fun speed1_5x_isDisplayed() {
        setPlayerScreen(speed = 1.5f)

        composeTestRule.onNodeWithText("1.5x").assertExists()
    }

    @Test
    fun speed2x_isDisplayed() {
        setPlayerScreen(speed = 2f)

        composeTestRule.onNodeWithText("2x").assertExists()
    }

    // --- New: Callback tests (top app bar buttons) ---

    @Test
    fun onCollapse_clickDownInvokesCallback() {
        var called = false
        setPlayerScreen(onCollapse = { called = true })

        composeTestRule.onNodeWithContentDescription("Down").performClick()
        assert(called)
    }

    @Test
    fun onToggleLike_clickLikeInvokesCallback() {
        var called = false
        setPlayerScreen(onToggleLike = { called = true })

        composeTestRule.onNodeWithContentDescription("Like").performClick()
        assert(called)
    }

    // --- New: Liked episode still renders ---

    @Test
    fun whenEpisodeIsLiked_likeActionStillExists() {
        val likedEpisode = episodeTestData.copy(likedAt = Instant.fromEpochSeconds(1000L))
        setPlayerScreen(nowPlaying = likedEpisode)

        // The action icon in the top bar always has "Like" content description
        composeTestRule.onNodeWithContentDescription("Like").assertExists()
    }

    // --- New: Control button existence ---

    @Test
    fun saveButtonExists() {
        setPlayerScreen()

        composeTestRule.onNodeWithContentDescription("Save").assertExists()
    }

    @Test
    fun listButtonExists() {
        setPlayerScreen()

        composeTestRule.onNodeWithContentDescription("List").assertExists()
    }

    // --- New: Empty playlist ---

    @Test
    fun emptyPlaylist_playerStillRendered() {
        setPlayerScreen(playlist = emptyList())

        composeTestRule.onNodeWithText(episodeTestData.title, substring = true).assertIsDisplayed()
    }

    // --- New: Progress display ---

    @Test
    fun durationTimeIsDisplayed() {
        setPlayerScreen(progress = Progress(0.seconds, 0.seconds, 3600.seconds))

        composeTestRule.onNodeWithText("1:00:00", substring = true).assertExists()
    }

    // --- New: Caption toggle button ---

    @Test
    fun inactiveCaptionButton_contentDescriptionIsTurnOn() {
        setPlayerScreen(captionButtonState = { CaptionButtonState.Inactive })

        composeTestRule.onNodeWithContentDescription("Turn on captions").assertExists()
    }

    @Test
    fun activeCaptionButton_contentDescriptionIsTurnOff() {
        setPlayerScreen(captionButtonState = { CaptionButtonState.Active })

        composeTestRule.onNodeWithContentDescription("Turn off captions").assertExists()
    }

    @Test
    fun captionButton_hasClickAction() {
        // 컨트롤 바 아래쪽 버튼들(List/Save/Share 도 동일)은 이 테스트 하네스의 뷰포트에서
        // 실측 터치 좌표가 0 크기로 잡혀 performClick() 제스처가 닿지 않는다 — 기존 List/Save/
        // Share 도 클릭이 아니라 존재만 검증한다. 여기서도 같은 이유로 클릭 대신 클릭 액션
        // 존재만 확인한다.
        setPlayerScreen()

        composeTestRule.onNode(hasScrollAction())
            .performScrollToNode(hasContentDescription("Turn on captions"))
        composeTestRule.onNodeWithContentDescription("Turn on captions").assertHasClickAction()
    }

    // --- New: Caption overlay vs. transcript cue ---

    @Test
    fun captionEpisodeIdMismatch_cueIsShownInstead() {
        val caption = liveCaptionTestData.copy(episodeId = 999L)
        setPlayerScreen(
            progress = Progress(1000.seconds, 2000.seconds, 6000.seconds, episodeId = 111L),
            liveCaption = { caption },
            cue = "test cue text",
        )

        composeTestRule.onNodeWithText("test cue text").assertExists()
        composeTestRule.onNodeWithText(caption.lines.last().text, substring = true).assertDoesNotExist()
    }

    @Test
    fun captionEpisodeIdMatches_captionTextIsShownInsteadOfCue() {
        val caption = liveCaptionTestData.copy(episodeId = 111L)
        setPlayerScreen(
            progress = Progress(1000.seconds, 2000.seconds, 6000.seconds, episodeId = 111L),
            liveCaption = { caption },
            cue = "test cue text",
        )

        composeTestRule.onNodeWithText(caption.lines.last().text, substring = true).assertExists()
        composeTestRule.onNodeWithText("test cue text").assertDoesNotExist()
    }

    @Test
    fun captionEpisodeIdMatches_translationLineIsShown() {
        val caption = liveCaptionTestData.copy(episodeId = 111L, isTranslating = true)
        setPlayerScreen(
            progress = Progress(1000.seconds, 2000.seconds, 6000.seconds, episodeId = 111L),
            liveCaption = { caption },
            cue = "test cue text",
        )

        composeTestRule.onNodeWithText(caption.translations.last().text, substring = true).assertExists()
    }

    @Test
    fun captionIsTranslatingFalse_translationLineIsNotShown() {
        val caption = liveCaptionTestData.copy(episodeId = 111L, isTranslating = false)
        setPlayerScreen(
            progress = Progress(1000.seconds, 2000.seconds, 6000.seconds, episodeId = 111L),
            liveCaption = { caption },
            cue = "test cue text",
        )

        composeTestRule.onNodeWithText(caption.translations.last().text, substring = true).assertDoesNotExist()
    }

    // --- New: Rolling caption display ---

    @Test
    fun rollingCaption_recentLinesAreShown() {
        val caption = liveCaptionTestData.copy(episodeId = 111L)
        setPlayerScreen(
            progress = Progress(1000.seconds, 2000.seconds, 6000.seconds, episodeId = 111L),
            liveCaption = { caption },
        )

        // 최근 두 줄(마지막 두 개)은 앨범아트 전체를 쓰는 롤링 영역 안에 항상 보여야 한다.
        composeTestRule.onNodeWithText(caption.lines[1].text, substring = true).assertExists()
        composeTestRule.onNodeWithText(caption.lines[2].text, substring = true).assertExists()
    }

    @Test
    fun rollingCaption_translationPersistsAcrossNewLine() {
        var caption by mutableStateOf(liveCaptionTestData.copy(episodeId = 111L))
        setPlayerScreen(
            progress = Progress(1000.seconds, 2000.seconds, 6000.seconds, episodeId = 111L),
            liveCaption = { caption },
        )

        val existingTranslation = caption.translations.last().text
        composeTestRule.onNodeWithText(existingTranslation, substring = true).assertExists()

        // 원문에 새 줄이 추가돼도(=lines 가 바뀌어도) translations 는 코어가 다음 번역이 올
        // 때까지 유지해 주는 값이다 — 화면이 lines 변화에 맞춰 리셋하지 않는지 확인한다.
        val newLine = captionLineOf(id = 4L, text = "A brand new line just arrived.", isFinal = false)
        caption = caption.copy(lines = caption.lines + newLine)
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText(newLine.text, substring = true).assertExists()
        composeTestRule.onNodeWithText(existingTranslation, substring = true).assertExists()
    }

    @Test
    fun rollingCaption_linesSharingUtteranceIdAreJoinedIntoOneParagraph() {
        // 강제 컷으로 나뉜 두 줄이 같은 발화(utteranceId)를 공유하면 화면은 이를 한 문단으로
        // 이어 그린다(captionParagraphs) — 두 줄이 공백 하나로 이어붙은 텍스트가 한 노드로 있어야 한다.
        val firstHalf = captionLineTestData.copy(id = 10L, utteranceId = 999L, text = "First half of the line")
        val secondHalf = captionLineTestData.copy(id = 11L, utteranceId = 999L, text = "second half.", isFinal = false)
        val caption = liveCaptionTestData.copy(
            episodeId = 111L,
            lines = listOf(firstHalf, secondHalf),
            translations = emptyList(),
            isTranslating = false,
        )
        setPlayerScreen(
            progress = Progress(1000.seconds, 2000.seconds, 6000.seconds, episodeId = 111L),
            liveCaption = { caption },
        )

        composeTestRule.onNodeWithText("${firstHalf.text} ${secondHalf.text}", substring = true).assertExists()
    }

    @Test
    fun toggledOffLongCue_isNotTruncated() {
        // 토글이 꺼진 기존 VTT 표시(PushUpCue)는 PR#116 이전처럼 줄 수 제한이 없어야 한다.
        // Robolectric 의 텍스트 측정은 실기기 폰트 셰이핑과 달라 실제 줄바꿈 개수·픽셀
        // 높이로는 잘림 여부를 신뢰성 있게 가를 수 없다(Ellipsis 는 의미 트리의 텍스트
        // 자체도 지우지 않는다). 대신 `GetTextLayoutResult` 시맨틱스 액션으로 실제 Text 에
        // 걸린 `maxLines` 설정값을 직접 읽어 제한이 없는지(Int.MAX_VALUE) 확인한다 — 이게
        // PR#116 의 `maxLines = 2` 회귀를 직접 가리키는 값이다.
        val longCue = "word ".repeat(60).trim()
        setPlayerScreen(
            liveCaption = { null },
            cue = longCue,
        )

        val node = composeTestRule.onNodeWithText(longCue, substring = true).fetchSemanticsNode()
        val results = mutableListOf<TextLayoutResult>()
        node.config[SemanticsActions.GetTextLayoutResult].action?.invoke(results)

        assertEquals(Int.MAX_VALUE, results.first().layoutInput.maxLines)
    }
}
