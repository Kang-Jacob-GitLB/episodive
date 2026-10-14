package io.jacob.episodive.core.network.di

import javax.inject.Qualifier

/**
 * 방송사(KBS·MBC·SBS) 스트림 주소 조회 전용 [okhttp3.OkHttpClient] 구분자.
 *
 * Podcast Index 인증 인터셉터(키 유출)도, Radio Browser 인터셉터(호스트 교체)도 달지 않는다.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class BroadcasterOkHttpClient
