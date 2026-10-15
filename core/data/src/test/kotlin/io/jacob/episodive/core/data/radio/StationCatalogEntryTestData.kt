// StationCatalogEntry 가 :core:data internal 이라 :core:testing 에 둘 수 없어 여기 둔다.
// (CLAUDE.md 의 :core:model 테스트 예외와 같은 사유 — 팩토리를 함께 쓰려면 모듈을 떼어내야 한다.)
package io.jacob.episodive.core.data.radio

import io.jacob.episodive.core.network.model.BroadcasterSource
import io.jacob.episodive.core.network.model.OnAirSource
import io.jacob.episodive.core.testing.model.kbsFirstRadioStationTestData
import io.jacob.episodive.core.testing.model.kbsHappyRadioStationTestData
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

/** 방송사 소스가 있는 HLS 항목. 공용 목록에는 넣지 않고 방송사 경로 테스트만 따로 쓴다. */
internal val broadcasterRadioEntryTestData = StationCatalogEntry(
    station = kbsFirstRadioStationTestData,
    radioBrowserIds = listOf("k"),
    isHls = true,
    fallbackUrl = "https://fallback/k",
    broadcasterSource = BroadcasterSource.Kbs("21"),
)

/** 편성표 소스(KBS)가 있는 항목. 공용 목록에는 넣지 않고 편성 테스트만 따로 쓴다. */
internal val onAirKbsEntryTestData = StationCatalogEntry(
    station = kbsFirstRadioStationTestData,
    radioBrowserIds = listOf("k"),
    isHls = true,
    fallbackUrl = null,
    onAirSource = OnAirSource.Kbs("21"),
)

/** 곡을 주는 MBC 편성표 소스가 있는 항목. */
internal val onAirMbcEntryTestData = StationCatalogEntry(
    station = kbsHappyRadioStationTestData,
    radioBrowserIds = listOf("m"),
    isHls = true,
    fallbackUrl = null,
    onAirSource = OnAirSource.Mbc("FM4U"),
)

internal val stationCatalogEntryTestData = listOf(
    fallbackRadioEntryTestData,
    tvEntryTestData,
    hlsRadioEntryTestData,
)
