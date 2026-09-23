package io.jacob.episodive.core.caption.engine

import io.jacob.episodive.core.model.caption.LiveCaption
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CaptionPresenterTest {
    private var now = 0L
    private val presenter = CaptionPresenter(clock = { now }, holdMillis = 1_500L)

    private fun caption(lineId: Long, text: String, isFinal: Boolean) =
        LiveCaption(episodeId = 1L, lineId = lineId, text = text, isFinal = isFinal)

    @Test
    fun `a finalized line replaces the screen immediately`() {
        val shown = presenter.onFinalized(caption(0, "Hello.", isFinal = true))

        assertEquals("Hello.", shown?.text)
        assertEquals(shown, presenter.current())
    }

    @Test
    fun `partials arriving during the hold window are held back`() {
        presenter.onFinalized(caption(0, "Hello.", isFinal = true))

        now += 500 // 유지 시간(1.5s) 안
        val shown = presenter.onPartial(caption(1, "Next", isFinal = false))

        assertEquals("Hello.", shown?.text)
        assertEquals("확정 줄이 유지 중이면 화면이 바뀌면 안 된다", "Hello.", presenter.current()?.text)
    }

    @Test
    fun `a held partial surfaces once the hold window expires`() {
        presenter.onFinalized(caption(0, "Hello.", isFinal = true))
        now += 500
        presenter.onPartial(caption(1, "Next", isFinal = false))

        now += 1_500 // 유지 시간을 완전히 지난다
        val shown = presenter.tick()

        assertEquals("Next", shown?.text)
    }

    @Test
    fun `a partial after the hold window expires is shown right away`() {
        presenter.onFinalized(caption(0, "Hello.", isFinal = true))
        now += 2_000

        val shown = presenter.onPartial(caption(1, "Next", isFinal = false))

        assertEquals("Next", shown?.text)
    }

    @Test
    fun `translation for the currently displayed line is attached`() {
        presenter.onFinalized(caption(0, "Hello.", isFinal = true))

        val shown = presenter.onTranslation(lineId = 0, translation = "안녕.")

        assertEquals("안녕.", shown?.translation)
    }

    @Test
    fun `a translation for a line that already scrolled past is discarded`() {
        presenter.onFinalized(caption(0, "Hello.", isFinal = true))
        now += 2_000
        presenter.onFinalized(caption(1, "Bye.", isFinal = true))

        val result = presenter.onTranslation(lineId = 0, translation = "안녕.")

        assertNull("이미 지난 줄로 온 번역은 버려야 한다", result)
        assertNull("현재 줄의 번역도 함께 더럽혀지면 안 된다", presenter.current()?.translation)
    }

    @Test
    fun `clear wipes the screen for a segment change`() {
        presenter.onFinalized(caption(0, "Hello.", isFinal = true))

        presenter.clear()

        assertNull(presenter.current())
        assertNull("clear 뒤에는 만료 대기 중이던 partial 도 남아 있으면 안 된다", presenter.tick())
    }
}
