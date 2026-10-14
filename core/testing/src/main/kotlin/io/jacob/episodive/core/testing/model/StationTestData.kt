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
    language = "ko",
)

val regionalRadioStationTestData = Station(
    id = "test-regional-radio",
    name = "테스트 지역 라디오",
    broadcaster = "TEST",
    kind = StationKind.RADIO,
    region = "부산",
    language = "ko",
)

/** 같은 방송사(KBS)의 전국 방송 두 개. 방송사 안 이니셜·색 규칙을 보는 테스트가 쓴다. */
val kbsFirstRadioStationTestData = Station(
    id = "test-kbs-1",
    name = "KBS 1라디오",
    broadcaster = "KBS",
    kind = StationKind.RADIO,
    region = null,
    language = "ko",
)

val kbsHappyRadioStationTestData = Station(
    id = "test-kbs-happy",
    name = "KBS 해피FM",
    broadcaster = "KBS",
    kind = StationKind.RADIO,
    region = null,
    language = "ko",
)

val seoulRegionalRadioStationTestData = Station(
    id = "test-regional-seoul",
    name = "마포FM",
    broadcaster = "마포FM",
    kind = StationKind.RADIO,
    region = "서울",
    language = "ko",
)

val daeguRegionalRadioStationTestData = Station(
    id = "test-regional-daegu",
    name = "성서공동체FM",
    broadcaster = "성서공동체FM",
    kind = StationKind.RADIO,
    region = "대구",
    language = "ko",
)

val tvStationTestData = Station(
    id = "test-tv",
    name = "테스트 TV",
    broadcaster = "TEST",
    kind = StationKind.TV,
    region = null,
    language = "ko",
)

val stationTestData = listOf(
    radioStationTestData,
    tvStationTestData,
    regionalRadioStationTestData,
)

/** 전국 3개 + 지역 3개의 라디오 목록. 카탈로그 순서를 흉내 낸다. */
val radioStationsTestData = listOf(
    radioStationTestData,
    kbsFirstRadioStationTestData,
    kbsHappyRadioStationTestData,
    regionalRadioStationTestData,
    seoulRegionalRadioStationTestData,
    daeguRegionalRadioStationTestData,
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
