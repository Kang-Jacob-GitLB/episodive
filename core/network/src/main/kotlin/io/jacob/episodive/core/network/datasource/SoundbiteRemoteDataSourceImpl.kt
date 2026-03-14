package io.jacob.episodive.core.network.datasource

import io.jacob.episodive.core.network.api.SoundbiteApi
import io.jacob.episodive.core.network.model.SoundbiteResponse
import timber.log.Timber
import javax.inject.Inject

class SoundbiteRemoteDataSourceImpl @Inject constructor(
    val soundbiteApi: SoundbiteApi,
) : SoundbiteRemoteDataSource {
    override suspend fun getSoundbites(max: Int?): List<SoundbiteResponse> {
        Timber.i("getSoundbites max: $max")
        return soundbiteApi.getSoundbites(max = max).dataList
    }
}