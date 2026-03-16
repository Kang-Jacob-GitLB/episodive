package io.jacob.episodive.core.domain.usecase.player

import io.jacob.episodive.core.domain.repository.UserRepository
import javax.inject.Inject

class SaveLastPlayStateUseCase @Inject constructor(
    private val userRepository: UserRepository,
) {
    suspend operator fun invoke(
        episodeId: Long,
        index: Int,
        positionMs: Long,
        shuffle: Boolean,
        repeat: Int,
    ) {
        userRepository.saveLastPlayState(episodeId, index, positionMs, shuffle, repeat)
    }
}
