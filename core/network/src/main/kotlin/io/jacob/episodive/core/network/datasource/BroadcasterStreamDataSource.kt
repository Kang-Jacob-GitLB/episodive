package io.jacob.episodive.core.network.datasource

import io.jacob.episodive.core.network.model.BroadcasterSource

interface BroadcasterStreamDataSource {
    /**
     * 방송사 API 에서 새 서명 URL 을 받는다. https 가 아닌 결과(에러 페이지 등)는 null.
     * HTTP 실패·파싱 실패는 예외로 던진다.
     */
    suspend fun resolve(source: BroadcasterSource): String?
}
