package io.jacob.episodive.core.testing.model

import io.jacob.episodive.core.model.caption.CaptionAvailability
import io.jacob.episodive.core.model.caption.CaptionLanguage
import io.jacob.episodive.core.model.caption.LiveCaption
import io.jacob.episodive.core.model.caption.LiveCaptionState

val liveCaptionTestData = LiveCaption(
    episodeId = 5778530L,
    lineId = 1L,
    text = "After early nightfall the yellow lamps would light up.",
    translation = "이른 밤이 되면 노란 램프가 켜지곤 했다.",
    isFinal = true,
)

val liveCaptionTestDataList = List(3) { index ->
    liveCaptionTestData.copy(
        lineId = index + 1L,
        text = "${liveCaptionTestData.text} ${index + 1}",
    )
}

val liveCaptionStateTestData = LiveCaptionState(
    isEnabled = true,
    availability = CaptionAvailability.Ready(CaptionLanguage.ENGLISH),
    caption = liveCaptionTestData,
)
