package io.jacob.episodive.core.domain.usecase.channel

import io.jacob.episodive.core.domain.repository.ChannelRepository
import io.jacob.episodive.core.model.Channel
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetChannelsUseCase @Inject constructor(
    private val channelRepository: ChannelRepository,
) {
    operator fun invoke(): Flow<List<Channel>> {
        return channelRepository.getChannels()
    }
}