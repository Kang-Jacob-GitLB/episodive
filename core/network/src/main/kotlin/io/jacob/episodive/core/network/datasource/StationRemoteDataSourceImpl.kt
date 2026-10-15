package io.jacob.episodive.core.network.datasource

import io.jacob.episodive.core.network.api.RadioBrowserApi
import io.jacob.episodive.core.network.model.StationDetailsResponse
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

    override suspend fun getStationDetails(stationUuids: List<String>): List<StationDetailsResponse> {
        Timber.i("getStationDetails stationUuids: $stationUuids")
        return radioBrowserApi.getStationsByUuid(stationUuids.joinToString(","))
    }

    override suspend fun getStationsByCountry(countryCode: String): List<StationDetailsResponse> {
        Timber.i("getStationsByCountry countryCode: $countryCode")
        return radioBrowserApi.searchStations(countryCode, COUNTRY_SEARCH_LIMIT)
    }

    private companion object {
        const val COUNTRY_SEARCH_LIMIT = 200
    }
}
