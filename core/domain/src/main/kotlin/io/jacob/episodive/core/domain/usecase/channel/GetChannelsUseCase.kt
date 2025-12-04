package io.jacob.episodive.core.domain.usecase.channel

import io.jacob.episodive.core.domain.repository.ChannelRepository
import javax.inject.Inject

class GetChannelsUseCase @Inject constructor(
    private val channelRepository: ChannelRepository,
) {
    operator fun invoke() = channelRepository.getChannels()
}