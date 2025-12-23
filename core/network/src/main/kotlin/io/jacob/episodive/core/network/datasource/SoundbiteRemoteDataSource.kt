package io.jacob.episodive.core.network.datasource

import io.jacob.episodive.core.network.model.SoundbiteResponse

interface SoundbiteRemoteDataSource {
    suspend fun getSoundbites(max: Int? = null): List<SoundbiteResponse>
}