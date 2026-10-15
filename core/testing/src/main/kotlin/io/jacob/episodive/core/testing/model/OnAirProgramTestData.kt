package io.jacob.episodive.core.testing.model

import io.jacob.episodive.core.model.OnAirProgram

/** 시각을 모르는 기본 프로그램. 화면 테스트가 쓴다. */
val onAirProgramTestData = OnAirProgram(
    title = "아침 라디오",
    host = "홍길동",
    imageUrl = "https://img.example.com/program.jpg",
    startMillis = null,
    endMillis = null,
    song = null,
)

/** 진행자·이미지·곡이 없는 프로그램. */
val bareOnAirProgramTestData = OnAirProgram(
    title = "심야 라디오",
    host = null,
    imageUrl = null,
    startMillis = null,
    endMillis = null,
    song = null,
)

/** 시각이 있는 프로그램. 편성 경계를 보는 테스트가 쓴다. */
fun onAirProgramTestData(
    title: String,
    startMillis: Long,
    endMillis: Long,
    host: String? = null,
    imageUrl: String? = null,
    song: String? = null,
) = OnAirProgram(
    title = title,
    host = host,
    imageUrl = imageUrl,
    startMillis = startMillis,
    endMillis = endMillis,
    song = song,
)
