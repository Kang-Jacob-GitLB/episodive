// :core:testing 은 :core:model 만 의존해 Entity 를 담을 수 없다. 기존 DAO 테스트들은 엔티티를
// 인라인으로 만들지만, 그보다 팩토리가 낫다고 보고 StationDao 테스트용 팩토리는 여기 둔다.
package io.jacob.episodive.core.database.dao

import io.jacob.episodive.core.database.model.FavoriteStationEntity
import io.jacob.episodive.core.database.model.RecentStationEntity
import io.jacob.episodive.core.testing.model.radioStationTestData
import io.jacob.episodive.core.testing.model.regionalRadioStationTestData
import io.jacob.episodive.core.testing.model.tvStationTestData

private const val BASE_MILLIS = 1_700_000_000_000L

/** 밀리초 단위로 시각을 벌려 같은 초 안의 순서도 검증할 수 있다. */
internal fun favoriteStationEntityOf(stationId: String, millisAfterBase: Long = 0) =
    FavoriteStationEntity(stationId = stationId, favoritedAtMillis = BASE_MILLIS + millisAfterBase)

internal fun recentStationEntityOf(stationId: String, millisAfterBase: Long = 0) =
    RecentStationEntity(stationId = stationId, playedAtMillis = BASE_MILLIS + millisAfterBase)

internal val stationIdsTestData = listOf(
    radioStationTestData.id,
    tvStationTestData.id,
    regionalRadioStationTestData.id,
)
