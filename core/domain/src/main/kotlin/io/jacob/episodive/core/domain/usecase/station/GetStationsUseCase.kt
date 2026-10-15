package io.jacob.episodive.core.domain.usecase.station

import io.jacob.episodive.core.domain.repository.StationRepository
import io.jacob.episodive.core.domain.repository.UserRepository
import io.jacob.episodive.core.model.StationKind
import io.jacob.episodive.core.model.StationListing
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * 사용자 국가([io.jacob.episodive.core.model.UserData.country])의 전체 스테이션 목록.
 *
 * UserData 전체가 아니라 country 만 본다. 통째로 flatMapLatest 하면 재생 속도처럼 무관한 값이 바뀔 때마다
 * 흐름을 끊고 다시 구독해, 해외 목록 요청을 다시 돌린다([GetMyRandomEpisodesUseCase][io.jacob.episodive.core.domain.usecase.episode.GetMyRandomEpisodesUseCase] 와 같은 이유).
 * 해외 목록을 받지 못하면 흐름이 예외로 끝나므로 호출한 쪽이 처리한다.
 */
class GetStationsUseCase @Inject constructor(
    private val stationRepository: StationRepository,
    private val userRepository: UserRepository,
) {
    operator fun invoke(kind: StationKind): Flow<StationListing> {
        return userRepository.getUserData()
            .map { it.country }
            .distinctUntilChanged()
            .flatMapLatest { country -> stationRepository.getStationListing(kind, country) }
    }
}
