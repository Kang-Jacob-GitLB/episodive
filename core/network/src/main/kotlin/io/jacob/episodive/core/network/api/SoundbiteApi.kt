package io.jacob.episodive.core.network.api

import io.jacob.episodive.core.network.model.ResponseListWrapper
import io.jacob.episodive.core.network.model.SoundbiteResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface SoundbiteApi {
    @GET("recent/soundbites")
    suspend fun getSoundbites(
        @Query("max") max: Int? = null,
    ): ResponseListWrapper<SoundbiteResponse>
}