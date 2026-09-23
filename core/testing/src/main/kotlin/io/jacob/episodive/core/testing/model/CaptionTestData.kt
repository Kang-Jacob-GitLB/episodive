package io.jacob.episodive.core.testing.model

import io.jacob.episodive.core.model.caption.CaptionAvailability
import io.jacob.episodive.core.model.caption.CaptionLanguage
import io.jacob.episodive.core.model.caption.CaptionLine
import io.jacob.episodive.core.model.caption.LiveCaption
import io.jacob.episodive.core.model.caption.LiveCaptionState

val captionLineTestData = CaptionLine(
    id = 1L,
    text = "After early nightfall the yellow lamps would light up.",
    isFinal = true,
)

/**
 * 줄 하나가 곧 발화 하나인 줄(VTT cue, 단일 발화). `captionLineTestData.copy(id = …)` 는 원본의
 * `utteranceId` 를 그대로 물려받아, 기본값이 `utteranceId = id` 인 실제 줄과 어긋나므로 이걸 쓴다.
 */
fun captionLineOf(id: Long, text: String, isFinal: Boolean = true): CaptionLine =
    captionLineTestData.copy(id = id, text = text, isFinal = isFinal, utteranceId = id)

/** 확정 줄 여러 개가 쌓인 상태 — 롤링 표시 검증에 쓴다(오래된 → 최신). */
val captionLineTestDataList = List(3) { index ->
    // 줄마다 다른 발화다(각자 문단). 한 발화의 여러 줄이 필요하면 utteranceId 를 직접 맞춘다.
    captionLineOf(id = index + 1L, text = "${captionLineTestData.text} ${index + 1}")
}

/** [captionLineTestDataList] 각 줄의 번역 — id 가 원문 줄과 같다. */
val captionTranslationTestDataList = captionLineTestDataList.map { line ->
    line.copy(text = "이른 밤이 되면 노란 램프가 켜지곤 했다. ${line.id}")
}

val liveCaptionTestData = LiveCaption(
    episodeId = 5778530L,
    lines = captionLineTestDataList,
    translations = captionTranslationTestDataList,
    isTranslating = true,
)

val liveCaptionStateTestData = LiveCaptionState(
    isEnabled = true,
    availability = CaptionAvailability.Ready(CaptionLanguage.ENGLISH),
    caption = liveCaptionTestData,
)
