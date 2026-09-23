package io.jacob.episodive.feature.player

import io.jacob.episodive.core.testing.model.captionLineTestData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CaptionParagraphTest {

    private fun line(id: Long, utteranceId: Long, text: String) =
        captionLineTestData.copy(id = id, utteranceId = utteranceId, text = text)

    @Test
    fun `consecutive lines of the same utterance are joined into one paragraph`() {
        val lines = listOf(
            line(1, utteranceId = 10L, text = "a"),
            line(2, utteranceId = 10L, text = "b"),
        )

        val paragraphs = captionParagraphs(lines)

        assertEquals(listOf(CaptionParagraph(10L, "a b")), paragraphs)
    }

    @Test
    fun `a non-consecutive occurrence of the same utteranceId is not merged back into the earlier paragraph`() {
        // [u1, u1, u2, u1] — 세 번째 줄이 발화 경계(u2)를 지나 다시 u1 로 돌아오더라도, 이미
        // 끝난 첫 문단과 다시 합치면 안 된다. 문단은 오직 "바로 앞" 이 같은 발화일 때만 이어진다.
        val lines = listOf(
            line(1, utteranceId = 1L, text = "a"),
            line(2, utteranceId = 1L, text = "b"),
            line(3, utteranceId = 2L, text = "c"),
            line(4, utteranceId = 1L, text = "d"),
        )

        val paragraphs = captionParagraphs(lines)

        assertEquals(
            listOf(
                CaptionParagraph(1L, "a b"),
                CaptionParagraph(2L, "c"),
                CaptionParagraph(1L, "d"),
            ),
            paragraphs,
        )
    }

    @Test
    fun `blank lines are skipped entirely`() {
        val lines = listOf(
            line(1, utteranceId = 1L, text = "a"),
            line(2, utteranceId = 1L, text = "   "),
            line(3, utteranceId = 1L, text = "b"),
        )

        val paragraphs = captionParagraphs(lines)

        assertEquals(listOf(CaptionParagraph(1L, "a b")), paragraphs)
    }

    @Test
    fun `a paragraph made only of blank lines disappears`() {
        val lines = listOf(
            line(1, utteranceId = 1L, text = ""),
            line(2, utteranceId = 1L, text = "   "),
        )

        val paragraphs = captionParagraphs(lines)

        assertTrue(paragraphs.isEmpty())
    }

    @Test
    fun `an empty line list produces no paragraphs`() {
        val paragraphs = captionParagraphs(emptyList())

        assertTrue(paragraphs.isEmpty())
    }

    @Test
    fun `pieces are joined without a space when both sides are in an unspaced script`() {
        val lines = listOf(line(1, utteranceId = 10L, text = "今日は"), line(2, utteranceId = 10L, text = "天気です"))

        assertEquals(listOf(CaptionParagraph(10L, "今日は天気です")), captionParagraphs(lines))
    }

    @Test
    fun `a space is kept when only one side is in an unspaced script, and for Korean`() {
        val mixed = listOf(line(1, utteranceId = 10L, text = "東京"), line(2, utteranceId = 10L, text = "Tower"))
        val korean = listOf(line(3, utteranceId = 20L, text = "가나"), line(4, utteranceId = 20L, text = "다라"))

        assertEquals(listOf(CaptionParagraph(10L, "東京 Tower")), captionParagraphs(mixed))
        assertEquals(listOf(CaptionParagraph(20L, "가나 다라")), captionParagraphs(korean))
    }
}
