package io.jacob.episodive.core.network.di

import javax.inject.Qualifier

/**
 * Radio Browser 전용 [okhttp3.OkHttpClient] 구분자.
 *
 * 한정자 없는 클라이언트에는 `EpisodiveInterceptor` 가 모든 요청에 Podcast Index API 키를 싣는다.
 * Radio Browser 호출에 그것을 쓰면 키가 그대로 외부로 나간다.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class RadioBrowserOkHttpClient

/** Radio Browser 전용 [retrofit2.Retrofit] 구분자. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class RadioBrowserRetrofit
