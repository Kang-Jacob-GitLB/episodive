// StationDetailsResponse 는 :core:network 모델이라 :core:testing 에 둘 수 없어 여기 둔다.
// (StationCatalogEntryTestData 와 같은 사유.)
package io.jacob.episodive.core.data.radio

import io.jacob.episodive.core.network.model.StationDetailsResponse

/** `fallbackRadioEntryTestData`(후보 a, b) 에 대한 응답. `stationDetailsTestData` 로 합쳐진다. */
internal val stationDetailsResponsesTestData = listOf(
    StationDetailsResponse(
        stationUuid = "b",
        homepage = "https://other.example.com",
        tags = "music",
        codec = "MP3",
        bitrate = 64,
    ),
    StationDetailsResponse(
        stationUuid = "a",
        homepage = "https://radio.example.com",
        tags = "news, talk",
        codec = "AAC",
        bitrate = 128,
    ),
)
