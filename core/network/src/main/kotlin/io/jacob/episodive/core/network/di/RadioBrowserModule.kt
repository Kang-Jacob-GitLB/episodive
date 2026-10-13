package io.jacob.episodive.core.network.di

import com.google.gson.Gson
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.jacob.episodive.core.network.BuildConfig
import io.jacob.episodive.core.network.api.RadioBrowserApi
import io.jacob.episodive.core.network.util.RadioBrowserInterceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

private const val CONNECT_TIMEOUT_SECONDS = 8L
private const val READ_TIMEOUT_SECONDS = 10L
private const val WRITE_TIMEOUT_SECONDS = 10L

/** 미러 하나가 죽어 있을 때 다음 미러까지 시도해 볼 수 있는 전체 상한. */
private const val CALL_TIMEOUT_SECONDS = 30L

/** 인터셉터가 호스트를 갈아끼우므로 여기 값은 자리표시자다. */
private const val RADIO_BROWSER_BASE_URL = "https://de1.api.radio-browser.info/"

@Module
@InstallIn(SingletonComponent::class)
object RadioBrowserModule {
    @Provides
    @Singleton
    @RadioBrowserOkHttpClient
    fun provideRadioBrowserOkHttpClient(): OkHttpClient {
        val builder = OkHttpClient.Builder()
            .addInterceptor(RadioBrowserInterceptor())
            .connectTimeout(CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .writeTimeout(WRITE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .callTimeout(CALL_TIMEOUT_SECONDS, TimeUnit.SECONDS)

        if (BuildConfig.DEBUG) {
            builder.addInterceptor(
                HttpLoggingInterceptor().apply {
                    level = HttpLoggingInterceptor.Level.BASIC
                }
            )
        }

        return builder.build()
    }

    @Provides
    @Singleton
    @RadioBrowserRetrofit
    fun provideRadioBrowserRetrofit(
        @RadioBrowserOkHttpClient okHttpClient: OkHttpClient,
        gson: Gson,
    ): Retrofit = Retrofit.Builder()
        .baseUrl(RADIO_BROWSER_BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create(gson))
        .build()

    @Provides
    @Singleton
    fun provideRadioBrowserApi(
        @RadioBrowserRetrofit retrofit: Retrofit,
    ): RadioBrowserApi = retrofit.create(RadioBrowserApi::class.java)
}
