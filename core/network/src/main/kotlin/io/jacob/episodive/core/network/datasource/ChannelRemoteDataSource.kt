package io.jacob.episodive.core.network.datasource

import io.jacob.episodive.core.network.model.ChannelResponse

interface ChannelRemoteDataSource {
    suspend fun getChannelById(id: Long): ChannelResponse?
    suspend fun getChannels(): List<ChannelResponse>
}