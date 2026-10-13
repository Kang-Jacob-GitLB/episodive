package io.jacob.episodive.core.testing.model

import io.jacob.episodive.core.model.PlaybackFailure
import io.jacob.episodive.core.model.Station
import io.jacob.episodive.core.model.StationKind
import io.jacob.episodive.core.model.StationStream

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

val hlsStreamTestData = StationStream(
    url = "https://example.com/live/playlist.m3u8",
    isHls = true,
)

val icecastStreamTestData = StationStream(
    url = "https://example.com/live/stream",
    isHls = false,
)

val playbackFailureTestData = PlaybackFailure(
    cause = PlaybackFailure.Cause.Gone,
    stationId = radioStationTestData.id,
    streamUrl = icecastStreamTestData.url,
)
