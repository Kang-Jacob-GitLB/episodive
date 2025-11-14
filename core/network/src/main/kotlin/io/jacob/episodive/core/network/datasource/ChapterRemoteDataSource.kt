package io.jacob.episodive.core.network.datasource

import io.jacob.episodive.core.model.Chapter

interface ChapterRemoteDataSource {
    suspend fun fetchChapters(url: String): List<Chapter>
}