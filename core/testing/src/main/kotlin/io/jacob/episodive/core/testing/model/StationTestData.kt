package io.jacob.episodive.core.testing.model

import io.jacob.episodive.core.model.Station
import io.jacob.episodive.core.model.StationKind

val radioStationTestData = Station(
    id = "test-radio",
    name = "테스트 라디오",
    broadcaster = "TEST",
    kind = StationKind.RADIO,
    region = null,
)

val regionalRadioStationTestData = Station(
    id = "test-regional-radio",
    name = "테스트 지역 라디오",
    broadcaster = "TEST",
    kind = StationKind.RADIO,
    region = "부산",
)

val tvStationTestData = Station(
    id = "test-tv",
    name = "테스트 TV",
    broadcaster = "TEST",
    kind = StationKind.TV,
    region = null,
)

val stationTestData = listOf(
    radioStationTestData,
    tvStationTestData,
    regionalRadioStationTestData,
)
