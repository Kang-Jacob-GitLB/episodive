package io.jacob.episodive.core.caption.di

import javax.inject.Qualifier

/**
 * STT 모델 다운로드 전용 [okhttp3.OkHttpClient] 구분자.
 *
 * `:core:network` 의 클라이언트는 `EpisodiveInterceptor` 가 모든 요청에 Podcast Index API 키를
 * 싣는다 — 그 클라이언트를 그대로 재사용하면 그 키가 헤더에 실린 채 그대로 Hugging Face 로
 * 나간다. 모델 다운로드는 이 전용 클라이언트로만 하고 절대 섞지 않는다.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class CaptionOkHttpClient
