package io.jacob.episodive.core.data.util.updater

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.PagingSource
import io.jacob.episodive.core.data.util.query.CacheableQuery
import io.jacob.episodive.core.model.DataErrorException
import io.jacob.episodive.core.network.util.toDataError
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onStart
import kotlin.time.Clock
import kotlin.time.Instant

abstract class RemoteUpdater<Query : CacheableQuery, Response, Entity, Output : Any>(
    protected open val query: Query,
    private val backgroundRefresher: BackgroundRefresher,
) {
    companion object {
        private const val FETCH_SIZE = 1000
    }

    protected abstract suspend fun fetchFromRemote(fetchSize: Int = FETCH_SIZE): List<Response>
    protected abstract suspend fun convertToEntity(responses: List<Response>): List<Entity>
    protected abstract suspend fun replaceToLocal(entities: List<Entity>)

    /**
     * 이 쿼리로 캐시된 것 중 가장 오래된 시각. 캐시가 하나도 없으면 null.
     *
     * 만료 여부(Boolean)가 아니라 시각을 그대로 받는다. "캐시가 없다"와 "캐시가 오래됐다"는
     * 둘 다 갱신 대상이지만 **갱신에 실패했을 때 취할 행동이 정반대**라서, 한쪽으로 뭉뚱그리면
     * 그 판단을 할 수 없다.
     */
    protected abstract suspend fun getOldestCachedAt(): Instant?

    protected abstract fun getPagingSource(): PagingSource<Int, Output>
    protected abstract fun getFlowSource(count: Int): Flow<List<Output>>

    fun getPagingData(pagingConfig: PagingConfig): Flow<PagingData<Output>> {
        return Pager(
            config = pagingConfig,
            pagingSourceFactory = { getPagingSource() }
        ).flow
            .onStart { refreshIfNeeded() }
    }

    fun getFlowList(count: Int): Flow<List<Output>> {
        return getFlowSource(count)
            .onStart { refreshIfNeeded() }
    }

    /**
     * 만료됐으면 갱신한다. **다만 캐시가 있느냐에 따라 기다리는지가 갈린다.**
     *
     * 캐시가 있으면 그것을 먼저 보여주고 갱신은 뒤에서 돈다(stale-while-revalidate).
     * 예전에는 만료만으로 여기서 원격을 끝까지 기다렸는데, 그동안 `onStart` 가 아래 Flow 를
     * 붙잡고 있어 **DB 에 멀쩡히 있는 데이터까지 화면에서 감췄다.** 홈의 랜덤 에피소드는
     * 응답이 수 초에서 수십 초까지 걸려서, 지난번 목록을 그대로 들고 있으면서도 첫 화면의
     * 절반을 스켈레톤으로 덮었다. 실제로 겪은 문제다.
     *
     * 캐시가 아예 없을 때만 기다린다. 그때는 대신 보여줄 것이 없어 기다리는 것 말고 할 수
     * 있는 일이 없고, 실패하면 화면이 오류를 다뤄야 한다.
     */
    private suspend fun refreshIfNeeded() {
        val cachedAt = getOldestCachedAt()
        val expired = cachedAt == null || Clock.System.now() - cachedAt > query.timeToLive
        if (!expired) return

        if (cachedAt != null) {
            backgroundRefresher.refreshInBackground(query.key) { refresh() }
            return
        }

        // 보여줄 것이 없으니 기다린다. **다만 갱신 자체는 여기서 돌리지 않는다** — 이 자리는
        // `onStart` 안이라 화면 스코프에 매달려 있어, 사용자가 5초 안에 탭을 옮기면
        // `WhileSubscribed` 가 걷히며 요청이 잘린다. 돌아와도 처음부터 다시 시작해 다시
        // 잘리므로, 탭을 오가는 동안 첫 데이터를 **영영** 받지 못한 채 스켈레톤에 갇힌다.
        // refreshAndWait 는 앱 스코프에서 돌리고 대기만 여기에 건다.
        // 보여줄 캐시가 없으니 실패는 화면이 다루도록 올린다.
        awaitRefresh()
    }

    /**
     * 만료와 무관하게 지금 갱신하고 끝날 때까지 기다린다. 사용자가 당겨서 새로고침할 때 쓴다.
     *
     * [refreshIfNeeded] 와 같은 [BackgroundRefresher] 키를 쓴다. 만료로 뒤에서 이미 돌고 있는 갱신이
     * 있으면 새로 띄우지 않고 그것을 기다린다 — 당기기 직전에 시작된 요청이라면 그 결과로 끝난다. 같은 키에
     * 요청을 겹쳐 띄우지 않는다는 [BackgroundRefresher] 의 약속을 깨지 않기 위해서다. 실패는 [DataErrorException]
     * 으로 올린다. 캐시는 그대로 남으므로 화면은 목록을 유지한 채 실패만 알리면 된다.
     */
    suspend fun forceRefresh() = awaitRefresh()

    /**
     * 갱신을 앱 스코프에서 돌리고 끝날 때까지 기다린다. 실패의 원인은 여기서 한 번만 판별해 실어 보낸다 —
     * 화면마다 예외 종류를 다시 들여다보게 하면 판별 규칙이 여러 벌로 갈라진다.
     */
    private suspend fun awaitRefresh() {
        try {
            backgroundRefresher.refreshAndWait(query.key) { refresh() }
        } catch (e: CancellationException) {
            // 취소는 실패가 아니다. 여기서 삼키면 코루틴 취소가 전파되지 않는다.
            throw e
        } catch (e: Throwable) {
            throw DataErrorException(e.toDataError(), e)
        }
    }

    suspend fun refresh() {
        val remote = fetchFromRemote()
        val entity = convertToEntity(remote)
        replaceToLocal(entity)
    }
}
