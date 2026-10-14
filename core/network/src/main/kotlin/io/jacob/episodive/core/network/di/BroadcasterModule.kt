package io.jacob.episodive.core.network.di

import com.google.gson.Gson
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.jacob.episodive.core.common.Dispatcher
import io.jacob.episodive.core.common.EpisodiveDispatchers
import io.jacob.episodive.core.network.BuildConfig
import io.jacob.episodive.core.network.datasource.BroadcasterStreamDataSource
import io.jacob.episodive.core.network.datasource.BroadcasterStreamDataSourceImpl
import io.jacob.episodive.core.network.util.RadioBrowserInterceptor
import kotlinx.coroutines.CoroutineDispatcher
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

private const val BROADCASTER_CONNECT_TIMEOUT_SECONDS = 5L
private const val BROADCASTER_CALL_TIMEOUT_SECONDS = 8L

@Module
@InstallIn(SingletonComponent::class)
object BroadcasterModule {
    @Provides
    @Singleton
    @BroadcasterOkHttpClient
    fun provideBroadcasterOkHttpClient(): OkHttpClient {
        val builder = OkHttpClient.Builder()
            .addInterceptor { chain ->
                chain.proceed(
                    chain.request().newBuilder()
                        .header("User-Agent", RadioBrowserInterceptor.USER_AGENT)
                        .build()
                )
            }
            .connectTimeout(BROADCASTER_CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .callTimeout(BROADCASTER_CALL_TIMEOUT_SECONDS, TimeUnit.SECONDS)

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
    fun provideBroadcasterStreamDataSource(
        @BroadcasterOkHttpClient okHttpClient: OkHttpClient,
        gson: Gson,
        @Dispatcher(EpisodiveDispatchers.IO) ioDispatcher: CoroutineDispatcher,
    ): BroadcasterStreamDataSource {
        return BroadcasterStreamDataSourceImpl(
            callFactory = okHttpClient,
            gson = gson,
            ioDispatcher = ioDispatcher,
        )
    }
}
