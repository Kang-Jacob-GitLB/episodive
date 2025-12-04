package io.jacob.episodive.core.data.repository

import io.jacob.episodive.core.domain.repository.ChannelRepository
import io.jacob.episodive.core.model.Channel
import io.jacob.episodive.core.network.datasource.ChannelRemoteDataSource
import io.jacob.episodive.core.network.mapper.toChannel
import io.jacob.episodive.core.network.mapper.toChannels
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class ChannelRepositoryImpl @Inject constructor(
    private val remoteDataSource: ChannelRemoteDataSource,
) : ChannelRepository {
    override fun getChannelById(id: Long): Flow<Channel?> {
        return flow {
            val channel = remoteDataSource.getChannelById(id)?.toChannel()
            emit(channel)
        }
    }

    override fun getChannels(): Flow<List<Channel>> {
        return flow {
            val channels = remoteDataSource.getChannels().toChannels()
            emit(channels)
        }
    }
}