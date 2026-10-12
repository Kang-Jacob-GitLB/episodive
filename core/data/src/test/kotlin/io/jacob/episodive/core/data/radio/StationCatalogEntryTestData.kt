// StationCatalogEntry 가 :core:data internal 이라 :core:testing 에 둘 수 없어 여기 둔다.
// (CLAUDE.md 의 :core:model 테스트 예외와 같은 사유 — 팩토리를 함께 쓰려면 모듈을 떼어내야 한다.)
package io.jacob.episodive.core.data.radio

import io.jacob.episodive.core.testing.model.radioStationTestData
import io.jacob.episodive.core.testing.model.regionalRadioStationTestData
import io.jacob.episodive.core.testing.model.tvStationTestData

/** 후보 두 개와 fallback 이 있는 HLS 아닌 항목. */
internal val fallbackRadioEntryTestData = StationCatalogEntry(
    station = radioStationTestData,
    radioBrowserIds = listOf("a", "b"),
    isHls = false,
    fallbackUrl = "https://fallback/a",
)

internal val tvEntryTestData = StationCatalogEntry(
    station = tvStationTestData,
    radioBrowserIds = listOf("t"),
    isHls = true,
    fallbackUrl = null,
)

/** fallback 이 없는 HLS 항목. */
internal val hlsRadioEntryTestData = StationCatalogEntry(
    station = regionalRadioStationTestData,
    radioBrowserIds = listOf("r"),
    isHls = true,
    fallbackUrl = null,
)

internal val stationCatalogEntryTestData = listOf(
    fallbackRadioEntryTestData,
    tvEntryTestData,
    hlsRadioEntryTestData,
)
