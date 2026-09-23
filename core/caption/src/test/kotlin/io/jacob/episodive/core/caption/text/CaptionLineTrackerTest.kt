package io.jacob.episodive.core.caption.text

import io.jacob.episodive.core.model.caption.CaptionLanguage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CaptionLineTrackerTest {
    @Test
    fun `remaining text within the limit only produces a partial line`() {
        val tracker = CaptionLineTracker(CaptionLanguage.ENGLISH, maxLineChars = 80)

        val update = tracker.onTokens(listOf(" hello", " world"))

        assertNull(update.finalized)
        // 아직 아무것도 확정하지 않은, 이 발화의 진짜 시작이라 첫 글자가 대문자화된다.
        assertEquals("Hello world", update.partial.text)
        assertEquals(0L, update.partial.id)
        assertTrue(!update.partial.isFinal)
    }

    @Test
    fun `overflow cuts at the last word boundary that still fits, not mid-word`() {
        // "가나다" 3자 * 6단어 = 15자 + 공백 5 = 20자, maxLineChars 10 이면 두 단어("가나다 가나다")
        // 까지만 담긴다(6자). 세 번째 단어를 더하면 10자를 넘는다.
        val tracker = CaptionLineTracker(CaptionLanguage.KOREAN, maxLineChars = 10)
        val words = listOf(" 가나다", " 가나다", " 가나다", " 가나다")

        val update = tracker.onTokens(words)

        assertEquals("가나다 가나다", update.finalized?.text)
        assertTrue(update.finalized!!.isFinal)
        // 확정하지 못한 나머지가 partial 로 남는다.
        assertEquals("가나다 가나다", update.partial.text)
        assertEquals(update.finalized!!.id + 1, update.partial.id)
    }

    @Test
    fun `cutting right after a standalone whitespace token keeps both sides intact`() {
        // 공백 단독 토큰(' ')은 findCutIndex 가 보는 remaining[index] 자체에는 글자가 없다.
        // effectiveWordStarts 로 그 뒤 'ef' 를 경계로 잡지 못하면 컷이 밀려 "cdef" 처럼
        // 앞줄과 뒷줄 사이에서 단어가 들러붙는다.
        val tracker = CaptionLineTracker(CaptionLanguage.ENGLISH, maxLineChars = 5)
        val tokens = listOf(" ab", " cd", " ", "ef", " gh")

        val update = tracker.onTokens(tokens)

        assertEquals("Ab cd", update.finalized?.text)
        assertEquals("ef gh", update.partial.text)
    }

    @Test
    fun `a single word longer than the limit is not force-cut`() {
        val tracker = CaptionLineTracker(CaptionLanguage.KOREAN, maxLineChars = 3)

        val update = tracker.onTokens(listOf(" 가나다라마"))

        assertNull("한 단어만으로 이미 넘으면 이번 호출에서는 자르지 않는다", update.finalized)
        assertEquals("가나다라마", update.partial.text)
    }

    @Test
    fun `already committed tokens are not re-emitted on later calls`() {
        val tracker = CaptionLineTracker(CaptionLanguage.KOREAN, maxLineChars = 10)
        val firstCall = tracker.onTokens(listOf(" 가나다", " 가나다", " 가나다"))
        assertEquals("가나다 가나다", firstCall.finalized?.text)

        // 다음 decode 는 stream.tokens() 전체(발화 처음부터 누적)를 다시 넘긴다. 이미 확정한
        // 앞부분이 그대로 들어와도 finalized 로 다시 나오면 안 된다.
        val secondCall = tracker.onTokens(listOf(" 가나다", " 가나다", " 가나다", " 마"))

        assertNull(secondCall.finalized)
        assertEquals("가나다 마", secondCall.partial.text)
    }

    @Test
    fun `endpoint finalizes everything remaining and resets for the next utterance`() {
        val tracker = CaptionLineTracker(CaptionLanguage.ENGLISH, maxLineChars = 80)
        tracker.onTokens(listOf(" hello"))

        val finalLine = tracker.onEndpoint(listOf(" hello", " world"))

        // committedTokenCount 가 아직 0 이라(줄 길이 초과로 잘린 적이 없다) 이 발화의 진짜
        // 시작이라 첫 글자가 대문자화된다.
        assertEquals("Hello world", finalLine?.text)
        assertTrue(finalLine!!.isFinal)

        // 리셋됐으니 다음 발화는 다시 발화 시작(대문자화)부터 시작한다.
        val nextUtterance = tracker.onTokens(listOf(" next"))
        assertEquals("Next", nextUtterance.partial.text)
        assertTrue(nextUtterance.partial.id > finalLine.id)
    }

    @Test
    fun `endpoint with nothing left emits no line`() {
        val tracker = CaptionLineTracker(CaptionLanguage.ENGLISH, maxLineChars = 80)
        tracker.onTokens(listOf(" hi"))
        tracker.onEndpoint(listOf(" hi"))

        assertNull(tracker.onEndpoint(emptyList()))
    }

    @Test
    fun `segment reset also starts a new lineId without finalizing anything`() {
        val tracker = CaptionLineTracker(CaptionLanguage.ENGLISH, maxLineChars = 80)
        val before = tracker.onTokens(listOf(" partial"))

        tracker.reset()
        val after = tracker.onTokens(listOf(" fresh"))

        assertTrue(after.partial.id > before.partial.id)
        assertEquals("Fresh", after.partial.text)
    }

    @Test
    fun `a forced split line and the following partial share the same utteranceId`() {
        // maxLineChars 초과로 강제로 잘린 줄과, 그 뒤 이어지는 partial 은 같은 발화이므로
        // utteranceId 가 같아야 한다 — 화면이 이 둘을 한 문단으로 이어 그린다(CaptionParagraph).
        val tracker = CaptionLineTracker(CaptionLanguage.KOREAN, maxLineChars = 10)
        val words = listOf(" 가나다", " 가나다", " 가나다", " 가나다")

        val update = tracker.onTokens(words)

        assertEquals(update.finalized!!.utteranceId, update.partial.utteranceId)
    }

    @Test
    fun `onEndpoint starts the next utterance with a new, larger utteranceId equal to its first line id`() {
        val tracker = CaptionLineTracker(CaptionLanguage.ENGLISH, maxLineChars = 80)
        tracker.onTokens(listOf(" hello"))
        val finalLine = tracker.onEndpoint(listOf(" hello", " world"))
        requireNotNull(finalLine)

        val nextUtterance = tracker.onTokens(listOf(" next"))

        assertTrue(nextUtterance.partial.utteranceId > finalLine.utteranceId)
        assertEquals(nextUtterance.partial.id, nextUtterance.partial.utteranceId)
    }

    @Test
    fun `reset starts a new utterance`() {
        val tracker = CaptionLineTracker(CaptionLanguage.ENGLISH, maxLineChars = 80)
        val before = tracker.onTokens(listOf(" partial"))

        tracker.reset()
        val after = tracker.onTokens(listOf(" fresh"))

        assertTrue(after.partial.utteranceId > before.partial.utteranceId)
        assertEquals(after.partial.id, after.partial.utteranceId)
    }

    @Test
    fun `a line is never cut right before a punctuation-only token`() {
        // " ." 는 선행 공백을 달고 와 단어 시작처럼 보이지만 앞 단어에 붙는다. "알겠지"(3자)가
        // 정확히 상한이라 그 앞이 유일한 후보가 되는 경우에도 거기서 자르면 안 된다 — 화면이
        // 조각을 공백으로 이으면 "알겠지 . 다음" 이 된다.
        val tracker = CaptionLineTracker(CaptionLanguage.KOREAN, maxLineChars = 3)

        val update = tracker.onTokens(listOf(" 알겠", "지", " .", " 다음"))

        assertNull(update.finalized)
        assertEquals("알겠지. 다음", update.partial.text)
    }
}
