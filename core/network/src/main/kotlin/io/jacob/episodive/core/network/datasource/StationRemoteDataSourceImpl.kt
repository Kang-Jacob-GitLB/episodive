package io.jacob.episodive.core.network.datasource

import io.jacob.episodive.core.network.api.RadioBrowserApi
import timber.log.Timber
import javax.inject.Inject

class StationRemoteDataSourceImpl @Inject constructor(
    private val radioBrowserApi: RadioBrowserApi,
) : StationRemoteDataSource {
    override suspend fun resolveStreamUrl(stationUuid: String): String? {
        Timber.i("resolveStreamUrl stationUuid: $stationUuid")
        val response = radioBrowserApi.getStationUrl(stationUuid)
        return response.url?.takeIf { response.ok && it.isNotBlank() }
    }
}
