package io.jacob.episodive.core.domain.usecase.channel

import io.jacob.episodive.core.domain.repository.ChannelRepository
import javax.inject.Inject

class GetChannelByIdUseCase @Inject constructor(
    private val channelRepository: ChannelRepository,
) {
    operator fun invoke(id: Long) = channelRepository.getChannelById(id)
}