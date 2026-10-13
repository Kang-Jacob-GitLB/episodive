package io.jacob.episodive.core.testing.model

import io.jacob.episodive.core.model.StationDetails

val stationDetailsTestData = StationDetails(
    homepage = "https://radio.example.com",
    tags = listOf("news", "talk"),
    codec = "AAC",
    bitrateKbps = 128,
)

/** 음질만 아는 스테이션. 홈페이지·태그 줄은 숨겨진다. */
val codecOnlyStationDetailsTestData = StationDetails(
    homepage = null,
    tags = emptyList(),
    codec = "MP3",
    bitrateKbps = null,
)
