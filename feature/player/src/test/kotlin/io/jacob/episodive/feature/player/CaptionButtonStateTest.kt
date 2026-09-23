package io.jacob.episodive.feature.player

import io.jacob.episodive.core.model.caption.CaptionAvailability
import io.jacob.episodive.core.model.caption.CaptionLanguage
import io.jacob.episodive.core.model.caption.LiveCaptionState
import org.junit.Assert.assertEquals
import org.junit.Test

/** 토글 상태표(설계 문서)의 각 행을 그대로 검증하는 순수 매핑 테스트. */
class CaptionButtonStateTest {

    @Test
    fun `Given Downloading availability, When mapping, Then Progress is returned regardless of isEnabled`() {
        val state = LiveCaptionState(
            isEnabled = true,
            availability = CaptionAvailability.Downloading(CaptionLanguage.ENGLISH, 0.4f),
            caption = null,
        )

        val result = state.toCaptionButtonState()

        assertEquals(CaptionButtonState.Progress(0.4f), result)
    }

    @Test
    fun `Given disabled with Downloading availability, When mapping, Then Progress is still returned`() {
        // 다운로드 진행 중인 자리를 누르면 켜짐 여부와 무관하게 항상 취소된다 — 토글 상태표.
        val state = LiveCaptionState(
            isEnabled = false,
            availability = CaptionAvailability.Downloading(CaptionLanguage.ENGLISH, 0f),
            caption = null,
        )

        val result = state.toCaptionButtonState()

        assertEquals(CaptionButtonState.Progress(0f), result)
    }

    @Test
    fun `Given disabled, When mapping, Then Inactive is returned`() {
        val state = LiveCaptionState(
            isEnabled = false,
            availability = CaptionAvailability.Ready(CaptionLanguage.ENGLISH),
            caption = null,
        )

        assertEquals(CaptionButtonState.Inactive, state.toCaptionButtonState())
    }

    @Test
    fun `Given enabled and Ready, When mapping, Then Active is returned`() {
        val state = LiveCaptionState(
            isEnabled = true,
            availability = CaptionAvailability.Ready(CaptionLanguage.KOREAN),
            caption = null,
        )

        assertEquals(CaptionButtonState.Active, state.toCaptionButtonState())
    }

    @Test
    fun `Given enabled and Loading, When mapping, Then Active is returned`() {
        val state = LiveCaptionState(
            isEnabled = true,
            availability = CaptionAvailability.Loading(CaptionLanguage.KOREAN),
            caption = null,
        )

        assertEquals(CaptionButtonState.Active, state.toCaptionButtonState())
    }

    @Test
    fun `Given enabled and Unknown, When mapping, Then Active is returned`() {
        val state = LiveCaptionState(
            isEnabled = true,
            availability = CaptionAvailability.Unknown,
            caption = null,
        )

        assertEquals(CaptionButtonState.Active, state.toCaptionButtonState())
    }

    @Test
    fun `Given enabled and Transcript, When mapping, Then Active is returned`() {
        val state = LiveCaptionState(
            isEnabled = true,
            availability = CaptionAvailability.Transcript,
            caption = null,
        )

        assertEquals(CaptionButtonState.Active, state.toCaptionButtonState())
    }

    @Test
    fun `Given enabled and Unsupported, When mapping, Then Dimmed is returned`() {
        val state = LiveCaptionState(
            isEnabled = true,
            availability = CaptionAvailability.Unsupported,
            caption = null,
        )

        assertEquals(CaptionButtonState.Dimmed, state.toCaptionButtonState())
    }

    @Test
    fun `Given enabled and NotInstalled, When mapping, Then Inactive is returned`() {
        val state = LiveCaptionState(
            isEnabled = true,
            availability = CaptionAvailability.NotInstalled(CaptionLanguage.ENGLISH, 132_000_000L),
            caption = null,
        )

        assertEquals(CaptionButtonState.Inactive, state.toCaptionButtonState())
    }
}
