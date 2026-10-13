package io.jacob.episodive.core.player.datasource

import java.nio.ByteBuffer
import java.nio.charset.CharacterCodingException
import java.nio.charset.Charset
import java.nio.charset.CodingErrorAction

private const val STREAM_TITLE_KEY = "StreamTitle='"

/**
 * ICY 메타데이터 원문 바이트에서 곡 제목을 뽑는다.
 *
 * media3 의 `IcyInfo.title` 은 UTF-8 로 디코드하다 실패하면 Latin-1 로 떨어진다. EUC-KR 로 내보내는
 * 국내 방송은 그 경로에서 한글이 깨지므로, 제목 구간의 바이트를 직접 잘라 UTF-8(엄격) → CP949 순으로
 * 시도한다. 둘 다 안 되면 media3 가 푼 [fallbackTitle] 을 쓴다.
 *
 * 제목 끝은 media3 와 같이 `';` 로 본다. 제목 안의 따옴표·세미콜론은 둘이 붙어 있지 않는 한 살아남는다.
 *
 * @return 제목(공백 포함 가능). 제목 정보가 아예 없으면 null
 */
internal fun decodeIcyTitle(rawMetadata: ByteArray, fallbackTitle: String?): String? {
    // Latin-1 은 바이트와 글자가 1:1 이라 인덱스를 그대로 바이트 위치로 쓸 수 있다.
    val view = String(rawMetadata, Charsets.ISO_8859_1)
    val keyAt = view.indexOf(STREAM_TITLE_KEY)
    if (keyAt < 0) return fallbackTitle

    val start = keyAt + STREAM_TITLE_KEY.length
    val end = view.indexOf("';", start).takeIf { it >= 0 }
        ?: view.lastIndexOf('\'').takeIf { it >= start }
        ?: view.indexOf('\u0000', start).takeIf { it >= 0 }
        ?: view.length
    val bytes = rawMetadata.copyOfRange(start, end)

    return decodeStrict(bytes, Charsets.UTF_8)
        ?: decodeStrict(bytes, korean)
        ?: fallbackTitle
}

private val korean: Charset =
    if (Charset.isSupported("x-windows-949")) Charset.forName("x-windows-949") else Charset.forName("EUC-KR")

private fun decodeStrict(bytes: ByteArray, charset: Charset): String? = try {
    charset.newDecoder()
        .onMalformedInput(CodingErrorAction.REPORT)
        .onUnmappableCharacter(CodingErrorAction.REPORT)
        .decode(ByteBuffer.wrap(bytes))
        .toString()
} catch (e: CharacterCodingException) {
    null
}
