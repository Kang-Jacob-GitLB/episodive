package io.jacob.episodive.core.network.api

import io.jacob.episodive.core.network.model.ChaptersResponse
import retrofit2.http.GET
import retrofit2.http.Url

interface ChapterApi {
    @GET
    suspend fun fetchChapters(@Url url: String): ChaptersResponse
}