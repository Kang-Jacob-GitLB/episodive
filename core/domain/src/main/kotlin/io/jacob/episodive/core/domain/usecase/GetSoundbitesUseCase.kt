package io.jacob.episodive.core.domain.usecase

import io.jacob.episodive.core.domain.repository.FeedRepository
import io.jacob.episodive.core.model.Soundbite
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetSoundbitesUseCase @Inject constructor(
    private val feedRepository: FeedRepository,
) {
    operator fun invoke(max: Int): Flow<List<Soundbite>> {
        return feedRepository.getRecentSoundbites(max)
    }
}