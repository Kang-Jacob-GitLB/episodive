package io.jacob.episodive.core.caption.text

import io.jacob.episodive.core.model.caption.CaptionLanguage
import org.junit.Assert.assertEquals
import org.junit.Test

class CaptionTextNormalizerTest {

    @Test
    fun `Korean tokens with leading-space and bullet markers join into one spaced sentence`() {
        // 스모크 테스트에서 그대로 가져온 토큰(CLAUDE.md 라이브 자막 절 참고).
        val tokens = listOf(" 걔는", " 괜찮은", " 척", "하", "려", "구", " 애", " 쓰는")

        val text = CaptionTextNormalizer.normalize(CaptionLanguage.KOREAN, tokens, isUtteranceStart = true)

        assertEquals("걔는 괜찮은 척하려구 애 쓰는", text)
    }

    @Test
    fun `a standalone whitespace token still marks the next token as a new word`() {
        // 스모크 테스트에서 그대로 가져온 토큰(CLAUDE.md 라이브 자막 절 참고). ' ' 토큰 자체는
        // 피스가 비어 버려지지만, 그 뒤 '하진' 은 자기 앞에 공백이 없어도 새 단어여야 한다.
        val tokens = listOf(" 지하철", "에서", " 다리", "를", " 벌", "리고", " ", "하진", " 마", "라", ".")

        val text = CaptionTextNormalizer.normalize(CaptionLanguage.KOREAN, tokens, isUtteranceStart = true)

        assertEquals("지하철에서 다리를 벌리고 하진 마라.", text)
    }

    @Test
    fun `a standalone whitespace token separates fragments of a new English word`() {
        // 스모크 테스트에서 그대로 가져온 토큰. ' ' 뒤의 'QUA' 는 자기 앞에 공백이 없지만
        // 새 단어("quarter")의 시작이다.
        val tokens = listOf(
            " AFTER", " EARLY", " NIGHT", "FALL", " THE", " S", "QUA", "LI", "D", " ", "QUA", "R", "TER",
        )

        val text = CaptionTextNormalizer.normalize(CaptionLanguage.ENGLISH, tokens, isUtteranceStart = true)

        assertEquals("After early nightfall the squalid quarter", text)
    }

    @Test
    fun `bullet word boundary marker behaves the same as a leading space`() {
        val spaceTokens = listOf(" 애", " 쓰는")
        val bulletTokens = listOf("▁애", "▁쓰는")

        assertEquals(
            CaptionTextNormalizer.normalize(CaptionLanguage.KOREAN, spaceTokens, isUtteranceStart = true),
            CaptionTextNormalizer.normalize(CaptionLanguage.KOREAN, bulletTokens, isUtteranceStart = true),
        )
    }

    @Test
    fun `Korean strips the space before sentence punctuation`() {
        // '.' 토큰이 선행 공백을 달고 오는 경우(스모크 테스트에서 KO 는 '.' 가 가끔 나온다).
        val tokens = listOf(" 알겠", "지", " .")

        val text = CaptionTextNormalizer.normalize(CaptionLanguage.KOREAN, tokens, isUtteranceStart = true)

        assertEquals("알겠지.", text)
    }

    @Test
    fun `English output is lowercased and capitalized only at utterance start`() {
        val tokens = listOf(" AFTER", " EARLY", " NIGHTFALL")

        val midUtterance = CaptionTextNormalizer.normalize(CaptionLanguage.ENGLISH, tokens, isUtteranceStart = false)
        val utteranceStart = CaptionTextNormalizer.normalize(CaptionLanguage.ENGLISH, tokens, isUtteranceStart = true)

        assertEquals("after early nightfall", midUtterance)
        assertEquals("After early nightfall", utteranceStart)
    }

    @Test
    fun `standalone i and its contractions are capitalized in English`() {
        val tokens = listOf(" I", " THINK", " I'M", " SURE", " I'LL", " GO", " I'D", " SAY", " I'VE", " SEEN")

        val text = CaptionTextNormalizer.normalize(CaptionLanguage.ENGLISH, tokens, isUtteranceStart = false)

        assertEquals("I think I'm sure I'll go I'd say I've seen", text)
    }

    @Test
    fun `a lowercase i inside a longer word is not capitalized`() {
        val tokens = listOf(" IN", " TIME")

        val text = CaptionTextNormalizer.normalize(CaptionLanguage.ENGLISH, tokens, isUtteranceStart = false)

        assertEquals("in time", text)
    }

    @Test
    fun `empty token list normalizes to an empty string`() {
        assertEquals("", CaptionTextNormalizer.normalize(CaptionLanguage.ENGLISH, emptyList(), isUtteranceStart = true))
        assertEquals("", CaptionTextNormalizer.normalize(CaptionLanguage.KOREAN, emptyList(), isUtteranceStart = true))
    }
}
