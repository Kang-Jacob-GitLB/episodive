package io.jacob.episodive.core.caption.di

import android.content.Context
import android.os.SystemClock
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.jacob.episodive.core.caption.asset.CaptionModelDownloader
import io.jacob.episodive.core.caption.asset.CaptionModelManager
import io.jacob.episodive.core.caption.asset.CaptionModelStore
import io.jacob.episodive.core.caption.engine.CaptionClock
import io.jacob.episodive.core.caption.engine.CaptionThread
import io.jacob.episodive.core.common.ApplicationScope
import io.jacob.episodive.core.common.Dispatcher
import io.jacob.episodive.core.common.EpisodiveDispatchers
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.asCoroutineDispatcher
import okhttp3.OkHttpClient
import java.io.File
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object CaptionModule {

    /**
     * `:core:network` 클라이언트와 절대 공유하지 않는다 — 이유는 [CaptionOkHttpClient] 문서 참고.
     * 모델 파일이 수백 MB 라 read/call 타임아웃을 짧게 주면 정상 다운로드가 도중에 끊긴다.
     * `callTimeout` 을 아예 걸지 않는 것은 [CaptionModelDownloader] 가 자체적으로 코루틴 취소로
     * 중단을 처리하기 때문이다(요청 하나가 아니라 전체 다운로드 관점의 취소).
     */
    @Provides
    @Singleton
    @CaptionOkHttpClient
    fun provideCaptionOkHttpClient(): OkHttpClient = OkHttpClient.Builder()
        // Hugging Face 는 실제 파일을 302 로 CDN 도메인에 넘긴다 — 기본값이 true 이긴 하지만
        // 이 클라이언트의 존재 이유가 그 리다이렉트를 태우는 것이므로 명시한다.
        .followRedirects(true)
        .followSslRedirects(true)
        .connectTimeout(CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .readTimeout(READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .writeTimeout(WRITE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .build()

    /**
     * 라이브 자막 인식 루프 전용 단일 daemon 스레드. sherpa-onnx 네이티브 스트림이 단일 스레드
     * 전제라서([CaptionThread] 문서 참고) 앱 전역에서 이 디스패처 하나만 존재해야 한다 —
     * `@Singleton` 이 아니면 주입받는 자리마다 새 스레드가 생겨 그 전제가 깨진다. daemon 으로
     * 만드는 것은 앱 프로세스 종료를 이 스레드가 막지 않게 하기 위해서다.
     */
    @Provides
    @Singleton
    @CaptionThread
    fun provideCaptionDispatcher(): CoroutineDispatcher =
        Executors.newSingleThreadExecutor { runnable ->
            Thread(runnable, "CaptionRecognition").apply { isDaemon = true }
        }.asCoroutineDispatcher()

    /**
     * `System.currentTimeMillis()` 대신 단조 시계를 쓴다. [CaptionPresenter][io.jacob.episodive.core.caption.engine.CaptionPresenter]
     * 는 이 값을 "확정 줄을 띄운 뒤 얼마나 지났는지" 를 재는 데만 쓰고 어디에도 저장하거나
     * 벽시계와 비교하지 않는다 — 그런 상대 시간 측정에는 사용자가 시계를 바꾸거나 NTP 로
     * 보정되는 순간 시간이 거꾸로 흐를 수 있는 `currentTimeMillis` 보다 `elapsedRealtime` 이
     * 맞다(재생 위치 저장처럼 다른 값과 비교·영속하는 값이 아니므로 부팅 이후로만 유효해도
     * 무방하다).
     */
    @Provides
    fun provideCaptionClock(): CaptionClock = CaptionClock { SystemClock.elapsedRealtime() }

    @Provides
    @Singleton
    fun provideCaptionModelStore(@ApplicationContext context: Context): CaptionModelStore =
        CaptionModelStore(File(context.noBackupFilesDir, "caption-models"))

    @Provides
    @Singleton
    fun provideCaptionModelDownloader(
        store: CaptionModelStore,
        @CaptionOkHttpClient httpClient: OkHttpClient,
        @Dispatcher(EpisodiveDispatchers.IO) ioDispatcher: CoroutineDispatcher,
    ): CaptionModelDownloader = CaptionModelDownloader(store, httpClient, ioDispatcher)

    /**
     * 생성자가 [CaptionModelStore.pruneUnregistered] 로 동기 파일 IO 를 한다 — 여기서 그 IO 를
     * 피할 수는 없다(Provides 는 항상 동기 함수다). 대신 이 프로바이더 자체가 **요청 시점에만**
     * 불리도록, 소비자(`:core:data` 의 `CaptionRepositoryImpl`)가 `CaptionModelManager` 를 직접
     * 받지 않고 `dagger.Lazy<CaptionModelManager>` 로 받아 `.get()` 을 IO 디스패처 위에서만
     * 부른다 — 그러면 Dagger 가 이 프로바이더 호출 자체를 그 시점까지 미룬다(`DoubleCheck`).
     * "메인 스레드에서 생성되지 않게 하라" 는 요구를 만족하는 지점은 여기가 아니라 그 호출부다.
     */
    @Provides
    @Singleton
    fun provideCaptionModelManager(
        store: CaptionModelStore,
        downloader: CaptionModelDownloader,
        @ApplicationScope scope: CoroutineScope,
    ): CaptionModelManager = CaptionModelManager(store, downloader, scope)

    private const val CONNECT_TIMEOUT_SECONDS = 15L
    private const val READ_TIMEOUT_SECONDS = 30L
    private const val WRITE_TIMEOUT_SECONDS = 15L
}
