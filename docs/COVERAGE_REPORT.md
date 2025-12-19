# Code Coverage Report

Generated: 2025-12-19 10:24:18

---

## Overall Summary

| Module         | Line Coverage  | Branch Coverage  | Total Lines   | Covered    | Missed     |
|----------------|----------------|------------------|---------------|------------|------------|
| core:data      | 76.5%          | 41.8%            | 631           | 483        | 148        |
| core:database  | 82.2%          | 50.8%            | 6132          | 5043       | 1089       |
| core:datastore | 91.5%          | 60.0%            | 71            | 65         | 6          |
| core:domain    | 87.1%          | 57.1%            | 286           | 249        | 37         |
| core:network   | 81.1%          | 58.1%            | 486           | 394        | 92         |
| --------       | -------------- | ---------------- | ------------- | ---------  | --------   |
| **TOTAL**      | ** 82.0%**     | ** 50.4%**       | **  7606**    | **  6234** | **  1372** |

---

## Classes with Low Coverage (< 80%)

| Module         | Package                        | Class                          | Coverage | Covered/Total | Missed |
|----------------|--------------------------------|--------------------------------|----------|---------------|--------|
| core:network   | io.jacob.episodive.core.networ | FeedApi.kt                     | 0.0%     | 0/20          | 20     |
| core:network   | io.jacob.episodive.core.networ | PodcastApi.kt                  | 0.0%     | 0/4           | 4      |
| core:network   | io.jacob.episodive.core.networ | EpisodeApi.kt                  | 0.0%     | 0/21          | 21     |
| core:database  | io.jacob.episodive.core.databa | EpisodiveDatabase_AutoMigratio | 7.1%     | 2/28          | 26     |
| core:database  | io.jacob.episodive.core.databa | EpisodiveDatabase_AutoMigratio | 16.7%    | 2/12          | 10     |
| core:database  | io.jacob.episodive.core.databa | EpisodiveDatabase_AutoMigratio | 33.3%    | 1/3           | 2      |
| core:data      | io.jacob.episodive.core.data.u | ConnectivityManagerNetworkMoni | 45.5%    | 5/11          | 6      |
| core:data      | io.jacob.episodive.core.data.r | EpisodeRepositoryImpl.kt       | 56.2%    | 63/112        | 49     |
| core:data      | io.jacob.episodive.core.data.r | FeedRepositoryImpl.kt          | 57.4%    | 31/54         | 23     |
| core:data      | io.jacob.episodive.core.data.r | PodcastRepositoryImpl.kt       | 59.7%    | 37/62         | 25     |
| core:domain    | io.jacob.episodive.core.domain | GetLikedEpisodesUseCase.kt     | 75.0%    | 3/4           | 1      |
| core:datastore | io.jacob.episodive.core.datast | UserPreferencesStore.kt        | 79.2%    | 19/24         | 5      |

---

## Detailed Module Reports

### core:data - 76.5% Coverage

#### 📦 io.jacob.episodive.core.data.repository - 69.8%

| Class                           | Coverage | Lines Covered/Total | Branches Covered/Total |
|---------------------------------|----------|---------------------|------------------------|
| ❌ EpisodeRepositoryImpl.kt      | 56.2%    | 63/112              | N/A                    |
| ❌ FeedRepositoryImpl.kt         | 57.4%    | 31/54               | N/A                    |
| ❌ PodcastRepositoryImpl.kt      | 59.7%    | 37/62               | 0/8                    |
| ✅ UserRepositoryImpl.kt         | 90.9%    | 20/22               | 2/2                    |
| ✅ RecentSearchRepositoryImpl.kt | 92.3%    | 12/13               | N/A                    |
| ✅ PlayerRepositoryImpl.kt       | 98.5%    | 66/67               | N/A                    |
| ✅ ChannelRepositoryImpl.kt      | 100.0%   | 4/4                 | N/A                    |

#### 📦 io.jacob.episodive.core.data.util - 78.6%

| Class                                  | Coverage | Lines Covered/Total | Branches Covered/Total |
|----------------------------------------|----------|---------------------|------------------------|
| ❌ ConnectivityManagerNetworkMonitor.kt | 45.5%    | 5/11                | 0/4                    |
| ✅ ImageCacheInterceptor.kt             | 100.0%   | 17/17               | 4/4                    |

#### 📦 io.jacob.episodive.core.data.util.updater - 90.4%

| Class                            | Coverage | Lines Covered/Total | Branches Covered/Total |
|----------------------------------|----------|---------------------|------------------------|
| ⚠️ RecentNewFeedRemoteUpdater.kt | 83.3%    | 15/18               | 2/6                    |
| ⚠️ TrendingFeedRemoteUpdater.kt  | 86.4%    | 19/22               | 2/6                    |
| ⚠️ RecentFeedRemoteUpdater.kt    | 86.4%    | 19/22               | 2/6                    |
| ⚠️ SoundbiteRemoteUpdater.kt     | 87.0%    | 20/23               | 5/12                   |
| ✅ PodcastRemoteUpdater.kt        | 92.3%    | 24/26               | 17/26                  |
| ✅ EpisodeRemoteUpdater.kt        | 94.6%    | 35/37               | 17/22                  |
| ✅ RemoteUpdater.kt               | 100.0%   | 18/18               | 1/2                    |

### core:database - 82.2% Coverage

#### 📦 io.jacob.episodive.core.database - 51.8%

| Class                                      | Coverage | Lines Covered/Total | Branches Covered/Total |
|--------------------------------------------|----------|---------------------|------------------------|
| ❌ EpisodiveDatabase_AutoMigration_3_4_Impl | 7.1%     | 2/28                | N/A                    |
| ❌ EpisodiveDatabase_AutoMigration_2_3_Impl | 16.7%    | 2/12                | N/A                    |
| ❌ EpisodiveDatabase_AutoMigration_1_2_Impl | 33.3%    | 1/3                 | N/A                    |
| ✅ EpisodiveDatabase_Impl.kt                | 94.9%    | 37/39               | N/A                    |
| ✅ EpisodiveDatabase.kt                     | 100.0%   | 1/1                 | N/A                    |

#### 📦 io.jacob.episodive.core.database.dao - 90.7%

| Class                      | Coverage | Lines Covered/Total | Branches Covered/Total |
|----------------------------|----------|---------------------|------------------------|
| ⚠️ RecentSearchDao_Impl.kt | 83.3%    | 45/54               | 4/6                    |
| ⚠️ EpisodeDao_Impl.kt      | 89.8%    | 1292/1439           | 221/392                |
| ✅ PodcastDao_Impl.kt       | 90.7%    | 804/886             | 110/208                |
| ✅ FeedDao_Impl.kt          | 93.0%    | 480/516             | 42/74                  |
| ✅ EpisodeDao.kt            | 94.9%    | 37/39               | 2/2                    |
| ✅ FeedDao.kt               | 100.0%   | 20/20               | N/A                    |
| ✅ PodcastDao.kt            | 100.0%   | 14/14               | 2/2                    |

#### 📦 io.jacob.episodive.core.database.mapper - 96.3%

| Class                 | Coverage | Lines Covered/Total | Branches Covered/Total |
|-----------------------|----------|---------------------|------------------------|
| ✅ DatabaseMapperKt.kt | 96.3%    | 314/326             | 3/6                    |

#### 📦 io.jacob.episodive.core.database.util - 100.0%

| Class                     | Coverage | Lines Covered/Total | Branches Covered/Total |
|---------------------------|----------|---------------------|------------------------|
| ✅ DurationConverter.kt    | 100.0%   | 3/3                 | 3/4                    |
| ✅ InstantConverter.kt     | 100.0%   | 3/3                 | 4/4                    |
| ✅ MediumConverter.kt      | 100.0%   | 3/3                 | 2/4                    |
| ✅ EpisodeTypeConverter.kt | 100.0%   | 3/3                 | 2/4                    |
| ✅ CategoryConverter.kt    | 100.0%   | 3/3                 | N/A                    |

#### 📦 io.jacob.episodive.core.database.datasource - 100.0%

| Class                                | Coverage | Lines Covered/Total | Branches Covered/Total |
|--------------------------------------|----------|---------------------|------------------------|
| ✅ PodcastLocalDataSourceImpl.kt      | 100.0%   | 28/28               | N/A                    |
| ✅ EpisodeLocalDataSourceImpl.kt      | 100.0%   | 38/38               | 19/24                  |
| ✅ RecentSearchLocalDataSourceImpl.kt | 100.0%   | 9/9                 | N/A                    |
| ✅ FeedLocalDataSourceImpl.kt         | 100.0%   | 54/54               | N/A                    |

#### 📦 io.jacob.episodive.core.database.migration - 100.0%

| Class                  | Coverage | Lines Covered/Total | Branches Covered/Total |
|------------------------|----------|---------------------|------------------------|
| ✅ AutoMigration3to4.kt | 100.0%   | 1/1                 | N/A                    |
| ✅ AutoMigration2to3.kt | 100.0%   | 1/1                 | N/A                    |

### core:datastore - 91.5% Coverage

#### 📦 io.jacob.episodive.core.datastore.store - 79.2%

| Class                     | Coverage | Lines Covered/Total | Branches Covered/Total |
|---------------------------|----------|---------------------|------------------------|
| ❌ UserPreferencesStore.kt | 79.2%    | 19/24               | N/A                    |

#### 📦 io.jacob.episodive.core.datastore.datasource - 100.0%

| Class                              | Coverage | Lines Covered/Total | Branches Covered/Total |
|------------------------------------|----------|---------------------|------------------------|
| ✅ UserPreferencesDataSourceImpl.kt | 100.0%   | 14/14               | N/A                    |

#### 📦 io.jacob.episodive.core.datastore.mapper - 100.0%

| Class                  | Coverage | Lines Covered/Total | Branches Covered/Total |
|------------------------|----------|---------------------|------------------------|
| ✅ DataStoreMapperKt.kt | 100.0%   | 12/12               | N/A                    |

### core:domain - 87.1% Coverage

#### 📦 io.jacob.episodive.core.domain.usecase.episode - 98.5%

| Class                                    | Coverage | Lines Covered/Total | Branches Covered/Total |
|------------------------------------------|----------|---------------------|------------------------|
| ❌ GetLikedEpisodesUseCase.kt             | 75.0%    | 3/4                 | N/A                    |
| ✅ GetEpisodesByPodcastIdPagingUseCase.kt | 100.0%   | 4/4                 | N/A                    |
| ✅ GetLikedEpisodesPagingUseCase.kt       | 100.0%   | 3/3                 | N/A                    |
| ✅ IsLikedUseCase.kt                      | 100.0%   | 3/3                 | N/A                    |
| ✅ ToggleLikedUseCase.kt                  | 100.0%   | 3/3                 | N/A                    |
| ✅ GetAllPlayedEpisodesUseCase.kt         | 100.0%   | 8/8                 | N/A                    |
| ✅ GetLiveEpisodesUseCase.kt              | 100.0%   | 3/3                 | N/A                    |
| ✅ GetClipEpisodesPagingUseCase.kt        | 100.0%   | 4/4                 | N/A                    |
| ✅ GetAllPlayedEpisodesPagingUseCase.kt   | 100.0%   | 7/7                 | N/A                    |
| ✅ UpdatePlayedEpisodeUseCase.kt          | 100.0%   | 11/11               | 2/4                    |
| ✅ GetMyRandomEpisodesUseCase.kt          | 100.0%   | 4/4                 | N/A                    |
| ✅ GetPlayingEpisodesUseCase.kt           | 100.0%   | 8/8                 | N/A                    |
| ✅ GetRecentEpisodesUseCase.kt            | 100.0%   | 3/3                 | N/A                    |
| ✅ GetChaptersUseCase.kt                  | 100.0%   | 3/3                 | N/A                    |

#### 📦 io.jacob.episodive.core.domain.usecase.channel - 100.0%

| Class                      | Coverage | Lines Covered/Total | Branches Covered/Total |
|----------------------------|----------|---------------------|------------------------|
| ✅ GetChannelByIdUseCase.kt | 100.0%   | 3/3                 | N/A                    |
| ✅ GetChannelsUseCase.kt    | 100.0%   | 3/3                 | N/A                    |

#### 📦 io.jacob.episodive.core.domain.usecase.search - 100.0%

| Class                           | Coverage | Lines Covered/Total | Branches Covered/Total |
|---------------------------------|----------|---------------------|------------------------|
| ✅ UpsertRecentSearchUseCase.kt  | 100.0%   | 4/4                 | N/A                    |
| ✅ SearchUseCase.kt              | 100.0%   | 5/5                 | N/A                    |
| ✅ GetRecentSearchesUseCase.kt   | 100.0%   | 3/3                 | N/A                    |
| ✅ DeleteRecentSearchUseCase.kt  | 100.0%   | 4/4                 | N/A                    |
| ✅ ClearRecentSearchesUseCase.kt | 100.0%   | 4/4                 | N/A                    |

#### 📦 io.jacob.episodive.core.domain.usecase - 100.0%

| Class                     | Coverage | Lines Covered/Total | Branches Covered/Total |
|---------------------------|----------|---------------------|------------------------|
| ✅ FindInLibraryUseCase.kt | 100.0%   | 7/7                 | N/A                    |
| ✅ GetSoundbitesUseCase.kt | 100.0%   | 3/3                 | N/A                    |

#### 📦 io.jacob.episodive.core.domain.usecase.podcast - 100.0%

| Class                                      | Coverage | Lines Covered/Total | Branches Covered/Total |
|--------------------------------------------|----------|---------------------|------------------------|
| ✅ GetForeignTrendingPodcastsUseCase.kt     | 100.0%   | 5/5                 | N/A                    |
| ✅ GetPodcastUseCase.kt                     | 100.0%   | 3/3                 | N/A                    |
| ✅ GetMyTrendingPodcastsUseCase.kt          | 100.0%   | 4/4                 | N/A                    |
| ✅ GetRecentPodcastsUseCase.kt              | 100.0%   | 11/11               | N/A                    |
| ✅ GetRecommendedPodcastsUseCase.kt         | 100.0%   | 5/5                 | N/A                    |
| ✅ GetFollowedPodcastsPagingUseCase.kt      | 100.0%   | 4/4                 | N/A                    |
| ✅ GetPodcastsByChannelUseCase.kt           | 100.0%   | 3/3                 | N/A                    |
| ✅ GetPodcastsByFeedIdsParallellyUseCase.kt | 100.0%   | 3/3                 | N/A                    |
| ✅ GetLocalTrendingPodcastsUseCase.kt       | 100.0%   | 4/4                 | N/A                    |
| ✅ ToggleFollowedUseCase.kt                 | 100.0%   | 3/3                 | N/A                    |
| ✅ GetMyRecentPodcastsUseCase.kt            | 100.0%   | 4/4                 | N/A                    |
| ✅ GetFollowedPodcastsUseCase.kt            | 100.0%   | 4/4                 | N/A                    |
| ✅ GetTrendingPodcastsUseCase.kt            | 100.0%   | 11/11               | N/A                    |

#### 📦 io.jacob.episodive.core.domain.usecase.player - 100.0%

| Class                     | Coverage | Lines Covered/Total | Branches Covered/Total |
|---------------------------|----------|---------------------|------------------------|
| ✅ PlayEpisodesUseCase.kt  | 100.0%   | 10/10               | 10/10                  |
| ✅ PlayEpisodeUseCase.kt   | 100.0%   | 4/4                 | N/A                    |
| ✅ ResumeEpisodeUseCase.kt | 100.0%   | 5/5                 | N/A                    |

#### 📦 io.jacob.episodive.core.domain.usecase.user - 100.0%

| Class                               | Coverage | Lines Covered/Total | Branches Covered/Total |
|-------------------------------------|----------|---------------------|------------------------|
| ✅ ToggleCategoryUseCase.kt          | 100.0%   | 3/3                 | N/A                    |
| ✅ SetFirstLaunchOffUseCase.kt       | 100.0%   | 4/4                 | N/A                    |
| ✅ GetUserDataUseCase.kt             | 100.0%   | 3/3                 | N/A                    |
| ✅ SetSpeedUseCase.kt                | 100.0%   | 4/4                 | N/A                    |
| ✅ GetSelectableCategoriesUseCase.kt | 100.0%   | 4/4                 | N/A                    |
| ✅ GetPreferredCategoriesUseCase.kt  | 100.0%   | 3/3                 | N/A                    |

### core:network - 81.1% Coverage

#### 📦 io.jacob.episodive.core.network.api - 0.0%

| Class           | Coverage | Lines Covered/Total | Branches Covered/Total |
|-----------------|----------|---------------------|------------------------|
| ❌ FeedApi.kt    | 0.0%     | 0/20                | N/A                    |
| ❌ PodcastApi.kt | 0.0%     | 0/4                 | N/A                    |
| ❌ EpisodeApi.kt | 0.0%     | 0/21                | N/A                    |

#### 📦 io.jacob.episodive.core.network.util - 94.1%

| Class                       | Coverage | Lines Covered/Total | Branches Covered/Total |
|-----------------------------|----------|---------------------|------------------------|
| ✅ EpisodiveInterceptor.kt   | 92.9%    | 13/14               | N/A                    |
| ✅ EpisodiveInterceptorKt.kt | 100.0%   | 3/3                 | N/A                    |

#### 📦 io.jacob.episodive.core.network.mapper - 97.9%

| Class                | Coverage | Lines Covered/Total | Branches Covered/Total |
|----------------------|----------|---------------------|------------------------|
| ✅ NetworkMapperKt.kt | 97.9%    | 275/281             | 32/52                  |

#### 📦 io.jacob.episodive.core.network.datasource - 100.0%

| Class                            | Coverage | Lines Covered/Total | Branches Covered/Total |
|----------------------------------|----------|---------------------|------------------------|
| ✅ PodcastRemoteDataSourceImpl.kt | 100.0%   | 16/16               | N/A                    |
| ✅ ChapterRemoteDataSourceImpl.kt | 100.0%   | 9/9                 | N/A                    |
| ✅ FeedRemoteDataSourceImpl.kt    | 100.0%   | 29/29               | N/A                    |
| ✅ ChannelRemoteDataSourceImpl.kt | 100.0%   | 7/7                 | 3/6                    |
| ✅ EpisodeRemoteDataSourceImpl.kt | 100.0%   | 39/39               | 1/4                    |

---

## Recommendations

### 🔴 Priority: Zero Coverage Classes

These classes have no test coverage and should be prioritized:

- **core:network/FeedApi.kt** (20 lines)
- **core:network/PodcastApi.kt** (4 lines)
- **core:network/EpisodeApi.kt** (21 lines)

### 🟡 Medium Priority: Low Coverage Classes (< 50%)

- **core:database/EpisodiveDatabase_AutoMigration_3_4_Impl.kt** - 7.1% (26 lines missed)
- **core:database/EpisodiveDatabase_AutoMigration_2_3_Impl.kt** - 16.7% (10 lines missed)
- **core:database/EpisodiveDatabase_AutoMigration_1_2_Impl.kt** - 33.3% (2 lines missed)
- **core:data/ConnectivityManagerNetworkMonitor.kt** - 45.5% (6 lines missed)

