package io.jacob.episodive.core.network.datasource

import io.jacob.episodive.core.model.Chapter
import io.jacob.episodive.core.network.api.ChapterApi
import io.jacob.episodive.core.network.mapper.toChapters
import javax.inject.Inject

class ChapterRemoteDataSourceImpl @Inject constructor(
    private val chapterApi: ChapterApi,
) : ChapterRemoteDataSource {
    override suspend fun fetchChapters(url: String): List<Chapter> {
        return try {
            val response = chapterApi.fetchChapters(url)
            response.toChapters()
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }
}