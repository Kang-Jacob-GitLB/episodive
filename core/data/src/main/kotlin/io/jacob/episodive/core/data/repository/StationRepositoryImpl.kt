package io.jacob.episodive.core.data.repository

import io.jacob.episodive.core.data.radio.KoreanStationCatalog
import io.jacob.episodive.core.data.radio.StationCatalogEntry
import io.jacob.episodive.core.domain.repository.StationRepository
import io.jacob.episodive.core.model.Station
import io.jacob.episodive.core.model.StationKind
import io.jacob.episodive.core.model.StationStream
import io.jacob.episodive.core.network.datasource.StationRemoteDataSource
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import timber.log.Timber
import kotlin.coroutines.cancellation.CancellationException

class StationRepositoryImpl internal constructor(
    private val remoteDataSource: StationRemoteDataSource,
    private val catalog: List<StationCatalogEntry>,
    private val ioDispatcher: CoroutineDispatcher,
) : StationRepository {

    override fun getStations(kind: StationKind): Flow<List<Station>> {
        return flowOf(
            catalog.map { it.station }.filter { it.kind == kind }
        )
    }

    override suspend fun resolveStreamUrl(stationId: String): StationStream? = withContext(ioDispatcher) {
        val entry = catalog.find { it.station.id == stationId } ?: return@withContext null

        // 후보마다 OkHttp 의 callTimeout 이 걸려 있어도 후보가 여럿이면 합이 길어진다. 재생 버튼을
        // 누른 사용자를 그만큼 기다리게 하지 않도록 전체에 상한을 두고, 넘으면 fallback 으로 간다.
        val resolved = withTimeoutOrNull(RESOLVE_TIMEOUT_MILLIS) {
            for (uuid in entry.radioBrowserIds) {
                try {
                    remoteDataSource.resolveStreamUrl(uuid)?.let { return@withTimeoutOrNull it }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Timber.w(e, "resolveStreamUrl failed. stationId: $stationId, uuid: $uuid")
                }
            }
            null
        }

        (resolved ?: entry.fallbackUrl)?.let { StationStream(url = it, isHls = entry.isHls) }
    }

    private companion object {
        const val RESOLVE_TIMEOUT_MILLIS = 15_000L
    }
}
