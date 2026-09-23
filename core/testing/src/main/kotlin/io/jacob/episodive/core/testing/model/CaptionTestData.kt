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

/** 확정 줄 여러 개가 쌓인 상태 — 롤링 표시 검증에 쓴다(오래된 → 최신). */
val captionLineTestDataList = List(3) { index ->
    captionLineTestData.copy(
        id = index + 1L,
        text = "${captionLineTestData.text} ${index + 1}",
    )
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
