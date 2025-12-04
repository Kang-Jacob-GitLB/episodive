package io.jacob.episodive.core.domain.repository

import io.jacob.episodive.core.model.Channel
import kotlinx.coroutines.flow.Flow

interface ChannelRepository {
    fun getChannelById(id: Long): Flow<Channel?>
    fun getChannels(): Flow<List<Channel>>
}