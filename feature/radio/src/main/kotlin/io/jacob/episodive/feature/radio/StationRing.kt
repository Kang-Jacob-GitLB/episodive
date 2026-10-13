package io.jacob.episodive.feature.radio

import io.jacob.episodive.core.model.Station

/**
 * 이전/다음이 도는 순환 목록. 기준 스테이션이 즐겨찾기에 있고 즐겨찾기가 둘 이상이면 즐겨찾기(라디오 화면의
 * 순서 그대로), 아니면 카탈로그 전체다. 즐겨찾기가 하나뿐이면 돌 곳이 없어 전체로 넘어간다.
 *
 * 기준이 즐겨찾기 밖이면 전체를 돈다 — 즐겨찾기 목록에서 기준을 못 찾아 "이전/다음"이 죽는 일이 없게 한다.
 */
internal fun stationRing(
    anchorId: String,
    favorites: List<Station>,
    all: List<Station>,
): List<Station> =
    if (favorites.size >= 2 && favorites.any { it.id == anchorId }) favorites else all

/** [anchorId] 에서 [step] 칸 떨어진 스테이션. 끝에서는 반대 끝으로 돈다. 돌 곳이 없으면(2개 미만) null. */
internal fun List<Station>.neighbor(anchorId: String, step: Int): Station? {
    if (size < 2) return null
    val index = indexOfFirst { it.id == anchorId }
    if (index < 0) return null
    return this[Math.floorMod(index + step, size)]
}
