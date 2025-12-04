package io.jacob.episodive.core.network.api

import io.jacob.episodive.core.network.model.PodcastResponse
import io.jacob.episodive.core.network.model.ResponseListWrapper
import io.jacob.episodive.core.network.model.ResponseWrapper
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface PodcastApi {
    @GET("search/byterm")
    suspend fun searchPodcasts(
        @Query("q") query: String,
        @Query("max") max: Int? = null,
    ): ResponseListWrapper<PodcastResponse>

    @GET("podcasts/byfeedid")
    suspend fun getPodcastByFeedId(
        @Query("id") feedId: Long,
    ): ResponseWrapper<PodcastResponse>

    @GET("podcasts/byfeedurl")
    suspend fun getPodcastByFeedUrl(
        @Query("url") feedUrl: String,
    ): ResponseWrapper<PodcastResponse>

    @GET("podcasts/byguid")
    suspend fun getPodcastByGuid(
        @Query("guid") guid: String,
    ): ResponseWrapper<PodcastResponse>

    @GET("podcasts/bymedium")
    suspend fun getPodcastsByMedium(
        @Query("medium") medium: String,
        @Query("max") max: Int? = null,
    ): ResponseListWrapper<PodcastResponse>

    @POST("podcasts/batch/byguid")
    suspend fun getPodcastsByGuids(
        @Body guids: List<String>,
    ): ResponseListWrapper<PodcastResponse>
}