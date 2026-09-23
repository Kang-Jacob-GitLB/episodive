package io.jacob.episodive.core.caption.engine

import io.jacob.episodive.core.testing.model.captionLineTestData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CaptionPresenterTest {
    private val presenter = CaptionPresenter(episodeId = 1L, maxLines = 3)

    private fun line(id: Long, text: String, isFinal: Boolean) =
        captionLineTestData.copy(id = id, text = text, isFinal = isFinal)

    @Test
    fun `a finalized line is added to the display immediately`() {
        val shown = presenter.onFinalized(line(0, "Hello.", isFinal = true))

        assertEquals(listOf(line(0, "Hello.", isFinal = true)), shown?.lines)
        assertEquals(shown, presenter.current())
    }

    @Test
    fun `a partial line is appended after the finalized lines`() {
        presenter.onFinalized(line(0, "Hello.", isFinal = true))

        val shown = presenter.onPartial(line(1, "Wor", isFinal = false))

        assertEquals(
            listOf(line(0, "Hello.", isFinal = true), line(1, "Wor", isFinal = false)),
            shown?.lines,
        )
    }

    @Test
    fun `a partial update for the same id replaces it without touching finalized lines`() {
        presenter.onFinalized(line(0, "Hello.", isFinal = true))
        presenter.onPartial(line(1, "Wor", isFinal = false))

        val shown = presenter.onPartial(line(1, "World", isFinal = false))

        assertEquals(
            "finalized 줄은 건드리지 않고 partial 만 교체돼야 한다",
            listOf(line(0, "Hello.", isFinal = true), line(1, "World", isFinal = false)),
            shown?.lines,
        )
    }

    @Test
    fun `a blank partial is not stored as a line`() {
        presenter.onFinalized(line(0, "Hello.", isFinal = true))

        val shown = presenter.onPartial(line(1, "", isFinal = false))

        assertEquals(
            "빈 partial 은 한 줄을 차지하면 안 된다",
            listOf(line(0, "Hello.", isFinal = true)),
            shown?.lines,
        )
    }

    @Test
    fun `current is null when the only thing that arrived is a blank partial`() {
        presenter.onPartial(line(0, "   ", isFinal = false))

        assertNull(presenter.current())
    }

    @Test
    fun `only the oldest finalized line drops once more than the limit arrives`() {
        presenter.onFinalized(line(0, "One.", isFinal = true))
        presenter.onFinalized(line(1, "Two.", isFinal = true))
        presenter.onFinalized(line(2, "Three.", isFinal = true))

        val shown = presenter.onFinalized(line(3, "Four.", isFinal = true))

        assertEquals(
            "1번(One.)만 빠지고 나머지 세 줄은 남아야 한다",
            listOf(
                line(1, "Two.", isFinal = true),
                line(2, "Three.", isFinal = true),
                line(3, "Four.", isFinal = true),
            ),
            shown?.lines,
        )
    }

    @Test
    fun `translations accumulate in order`() {
        presenter.onFinalized(line(0, "Hello.", isFinal = true))
        presenter.onFinalized(line(1, "World.", isFinal = true))

        presenter.onTranslation(lineId = 0, text = "안녕.")
        val shown = presenter.onTranslation(lineId = 1, text = "세계.")

        assertEquals(
            listOf(line(0, "안녕.", isFinal = true), line(1, "세계.", isFinal = true)),
            shown?.translations,
        )
    }

    @Test
    fun `translations are bounded to maxLines just like the original lines`() {
        presenter.onFinalized(line(0, "One.", isFinal = true))
        presenter.onFinalized(line(1, "Two.", isFinal = true))
        presenter.onFinalized(line(2, "Three.", isFinal = true))
        presenter.onFinalized(line(3, "Four.", isFinal = true))

        presenter.onTranslation(lineId = 0, text = "하나.")
        presenter.onTranslation(lineId = 1, text = "둘.")
        presenter.onTranslation(lineId = 2, text = "셋.")
        val shown = presenter.onTranslation(lineId = 3, text = "넷.")

        assertEquals(
            listOf(line(1, "둘.", isFinal = true), line(2, "셋.", isFinal = true), line(3, "넷.", isFinal = true)),
            shown?.translations,
        )
    }

    @Test
    fun `isTranslating is passed through as given`() {
        val translating = CaptionPresenter(episodeId = 1L, isTranslating = true)

        val shown = translating.onFinalized(line(0, "Hello.", isFinal = true))

        assertTrue(shown?.isTranslating == true)
    }

    @Test
    fun `a translation survives new partial and finalized lines until the next translation arrives`() {
        presenter.onFinalized(line(0, "Hello.", isFinal = true))
        presenter.onTranslation(lineId = 0, text = "안녕.")

        presenter.onPartial(line(1, "Wor", isFinal = false))
        val afterPartial = presenter.current()
        presenter.onFinalized(line(1, "World.", isFinal = true))
        val afterFinalized = presenter.current()

        assertEquals(listOf(line(0, "안녕.", isFinal = true)), afterPartial?.translations)
        assertEquals(listOf(line(0, "안녕.", isFinal = true)), afterFinalized?.translations)
    }

    @Test
    fun `a translation for an older lineId is ignored`() {
        presenter.onFinalized(line(0, "Hello.", isFinal = true))
        presenter.onTranslation(lineId = 0, text = "안녕.")
        presenter.onFinalized(line(1, "Bye.", isFinal = true))
        presenter.onTranslation(lineId = 1, text = "잘가.")

        val result = presenter.onTranslation(lineId = 0, text = "늦게 온 안녕.")

        assertEquals(
            "늦은 번역은 버리고 기존 번역들을 유지해야 한다",
            listOf(line(0, "안녕.", isFinal = true), line(1, "잘가.", isFinal = true)),
            result?.translations,
        )
    }

    @Test
    fun `a translation for a line cleared before it arrives is ignored`() {
        presenter.onFinalized(line(0, "Hello.", isFinal = true))
        presenter.clear()
        presenter.onFinalized(line(1, "New segment.", isFinal = true))

        // 시크 전 문장(lineId=0)의 번역이 시크 이후에 도착한 상황.
        val result = presenter.onTranslation(lineId = 0, text = "늦게 온 안녕.")

        assertEquals(
            "지난 구간의 번역이 새 구간 밑에 붙으면 안 된다",
            emptyList<Any>(),
            result?.translations,
        )
    }

    @Test
    fun `clear wipes both lines and translations`() {
        presenter.onFinalized(line(0, "Hello.", isFinal = true))
        presenter.onTranslation(lineId = 0, text = "안녕.")

        presenter.clear()

        assertNull(presenter.current())
    }

    @Test
    fun `current is null when nothing has arrived yet`() {
        assertNull(presenter.current())
    }

    @Test
    fun `finalizePartial moves the in-flight partial to finalized and returns it`() {
        presenter.onFinalized(line(0, "Hello.", isFinal = true))
        presenter.onPartial(line(1, "Wor", isFinal = false))

        val finalized = presenter.finalizePartial()

        assertEquals(line(1, "Wor", isFinal = true), finalized)
        assertEquals(
            "partial 자리는 비고 확정 목록에 그 줄이 얹혀야 한다",
            listOf(line(0, "Hello.", isFinal = true), line(1, "Wor", isFinal = true)),
            presenter.current()?.lines,
        )
    }

    @Test
    fun `finalizePartial returns null when there is no partial in flight`() {
        presenter.onFinalized(line(0, "Hello.", isFinal = true))

        val finalized = presenter.finalizePartial()

        assertNull(finalized)
        assertEquals(listOf(line(0, "Hello.", isFinal = true)), presenter.current()?.lines)
    }

    @Test
    fun `finalizePartial does nothing when the only partial ever received was blank`() {
        presenter.onPartial(line(0, "   ", isFinal = false))

        val finalized = presenter.finalizePartial()

        assertNull(finalized)
        assertNull(presenter.current())
    }
}
