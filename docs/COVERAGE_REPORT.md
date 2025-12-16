# Code Coverage Report

Generated: 2025-12-16 17:30:42

---

## Overall Summary

| Module         | Line Coverage  | Branch Coverage  | Total Lines   | Covered    | Missed     |
|----------------|----------------|------------------|---------------|------------|------------|
| core:data      | 75.5%          | 36.8%            | 649           | 490        | 159        |
| core:database  | 79.0%          | 46.8%            | 6050          | 4778       | 1272       |
| core:datastore | 91.5%          | 60.0%            | 71            | 65         | 6          |
| core:domain    | 73.5%          | 50.0%            | 275           | 202        | 73         |
| core:network   | 81.1%          | 58.1%            | 486           | 394        | 92         |
| --------       | -------------- | ---------------- | ------------- | ---------  | --------   |
| **TOTAL**      | ** 78.7%**     | ** 46.3%**       | **  7531**    | **  5929** | **  1602** |

---

## Classes with Low Coverage (< 80%)

| Module         | Package                        | Class                          | Coverage | Covered/Total | Missed |
|----------------|--------------------------------|--------------------------------|----------|---------------|--------|
| core:network   | io.jacob.episodive.core.networ | FeedApi.kt                     | 0.0%     | 0/20          | 20     |
| core:network   | io.jacob.episodive.core.networ | PodcastApi.kt                  | 0.0%     | 0/4           | 4      |
| core:network   | io.jacob.episodive.core.networ | EpisodeApi.kt                  | 0.0%     | 0/21          | 21     |
| core:domain    | io.jacob.episodive.core.domain | GetChannelByIdUseCase.kt       | 0.0%     | 0/3           | 3      |
| core:domain    | io.jacob.episodive.core.domain | IsLikedUseCase.kt              | 0.0%     | 0/3           | 3      |
| core:domain    | io.jacob.episodive.core.domain | GetLikedEpisodesUseCase.kt     | 0.0%     | 0/4           | 4      |
| core:domain    | io.jacob.episodive.core.domain | GetAllPlayedEpisodesPagingUseC | 0.0%     | 0/3           | 3      |
| core:domain    | io.jacob.episodive.core.domain | GetPlayingEpisodesPagingUseCas | 0.0%     | 0/3           | 3      |
| core:domain    | io.jacob.episodive.core.domain | GetForeignTrendingPodcastsUseC | 0.0%     | 0/5           | 5      |
| core:domain    | io.jacob.episodive.core.domain | GetFollowedPodcastsPagingUseCa | 0.0%     | 0/4           | 4      |
| core:domain    | io.jacob.episodive.core.domain | GetPodcastsByFeedIdsParallelly | 0.0%     | 0/3           | 3      |
| core:domain    | io.jacob.episodive.core.domain | GetLocalTrendingPodcastsUseCas | 0.0%     | 0/4           | 4      |
| core:database  | io.jacob.episodive.core.databa | EpisodiveDatabase_AutoMigratio | 16.7%    | 2/12          | 10     |
| core:database  | io.jacob.episodive.core.databa | EpisodiveDatabase_AutoMigratio | 33.3%    | 1/3           | 2      |
| core:data      | io.jacob.episodive.core.data.u | ConnectivityManagerNetworkMoni | 45.5%    | 5/11          | 6      |
| core:data      | io.jacob.episodive.core.data.r | EpisodeRepositoryImpl.kt       | 53.8%    | 70/130        | 60     |
| core:data      | io.jacob.episodive.core.data.r | FeedRepositoryImpl.kt          | 57.4%    | 31/54         | 23     |
| core:data      | io.jacob.episodive.core.data.r | PodcastRepositoryImpl.kt       | 59.7%    | 37/62         | 25     |
| core:database  | io.jacob.episodive.core.databa | EpisodeDao_Impl.kt             | 72.0%    | 1020/1416     | 396    |
| core:datastore | io.jacob.episodive.core.datast | UserPreferencesStore.kt        | 79.2%    | 19/24         | 5      |

---

## Detailed Module Reports

### core:data - 75.5% Coverage

#### 📦 io.jacob.episodive.core.data.repository - 68.2%

| Class                           | Coverage | Lines Covered/Total | Branches Covered/Total |
|---------------------------------|----------|---------------------|------------------------|
| ❌ EpisodeRepositoryImpl.kt      | 53.8%    | 70/130              | 0/20                   |
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

### core:database - 79.0% Coverage

#### 📦 io.jacob.episodive.core.database - 70.8%

| Class                                      | Coverage | Lines Covered/Total | Branches Covered/Total |
|--------------------------------------------|----------|---------------------|------------------------|
| ❌ EpisodiveDatabase_AutoMigration_2_3_Impl | 16.7%    | 2/12                | N/A                    |
| ❌ EpisodiveDatabase_AutoMigration_1_2_Impl | 33.3%    | 1/3                 | N/A                    |
| ✅ EpisodiveDatabase_Impl.kt                | 93.8%    | 30/32               | N/A                    |
| ✅ EpisodiveDatabase.kt                     | 100.0%   | 1/1                 | N/A                    |

#### 📦 io.jacob.episodive.core.database.dao - 82.1%

| Class                      | Coverage | Lines Covered/Total | Branches Covered/Total |
|----------------------------|----------|---------------------|------------------------|
| ❌ EpisodeDao_Impl.kt       | 72.0%    | 1020/1416           | 153/360                |
| ⚠️ RecentSearchDao_Impl.kt | 83.3%    | 45/54               | 4/6                    |
| ✅ PodcastDao_Impl.kt       | 90.7%    | 804/886             | 110/208                |
| ✅ FeedDao_Impl.kt          | 93.0%    | 480/516             | 42/74                  |
| ✅ EpisodeDao.kt            | 100.0%   | 13/13               | 2/2                    |
| ✅ FeedDao.kt               | 100.0%   | 20/20               | N/A                    |
| ✅ PodcastDao.kt            | 100.0%   | 14/14               | 2/2                    |

#### 📦 io.jacob.episodive.core.database.mapper - 95.4%

| Class                 | Coverage | Lines Covered/Total | Branches Covered/Total |
|-----------------------|----------|---------------------|------------------------|
| ✅ DatabaseMapperKt.kt | 95.4%    | 332/348             | 3/6                    |

#### 📦 io.jacob.episodive.core.database.datasource - 99.2%

| Class                                | Coverage | Lines Covered/Total | Branches Covered/Total |
|--------------------------------------|----------|---------------------|------------------------|
| ✅ EpisodeLocalDataSourceImpl.kt      | 97.3%    | 36/37               | N/A                    |
| ✅ PodcastLocalDataSourceImpl.kt      | 100.0%   | 28/28               | N/A                    |
| ✅ RecentSearchLocalDataSourceImpl.kt | 100.0%   | 9/9                 | N/A                    |
| ✅ FeedLocalDataSourceImpl.kt         | 100.0%   | 54/54               | N/A                    |

#### 📦 io.jacob.episodive.core.database.util - 100.0%

| Class                     | Coverage | Lines Covered/Total | Branches Covered/Total |
|---------------------------|----------|---------------------|------------------------|
| ✅ DurationConverter.kt    | 100.0%   | 3/3                 | 3/4                    |
| ✅ InstantConverter.kt     | 100.0%   | 3/3                 | 4/4                    |
| ✅ MediumConverter.kt      | 100.0%   | 3/3                 | 2/4                    |
| ✅ EpisodeTypeConverter.kt | 100.0%   | 3/3                 | 2/4                    |
| ✅ SoundbiteConverter.kt   | 100.0%   | 4/4                 | N/A                    |
| ✅ CategoryConverter.kt    | 100.0%   | 3/3                 | N/A                    |

#### 📦 io.jacob.episodive.core.database.migration - 100.0%

| Class                  | Coverage | Lines Covered/Total | Branches Covered/Total |
|------------------------|----------|---------------------|------------------------|
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

### core:domain - 73.5% Coverage

#### 📦 io.jacob.episodive.core.domain.usecase.channel - 50.0%

| Class                      | Coverage | Lines Covered/Total | Branches Covered/Total |
|----------------------------|----------|---------------------|------------------------|
| ❌ GetChannelByIdUseCase.kt | 0.0%     | 0/3                 | N/A                    |
| ✅ GetChannelsUseCase.kt    | 100.0%   | 3/3                 | N/A                    |

#### 📦 io.jacob.episodive.core.domain.usecase.podcast - 75.0%

| Class                                      | Coverage | Lines Covered/Total | Branches Covered/Total |
|--------------------------------------------|----------|---------------------|------------------------|
| ❌ GetForeignTrendingPodcastsUseCase.kt     | 0.0%     | 0/5                 | N/A                    |
| ❌ GetFollowedPodcastsPagingUseCase.kt      | 0.0%     | 0/4                 | N/A                    |
| ❌ GetPodcastsByFeedIdsParallellyUseCase.kt | 0.0%     | 0/3                 | N/A                    |
| ❌ GetLocalTrendingPodcastsUseCase.kt       | 0.0%     | 0/4                 | N/A                    |
| ✅ GetPodcastUseCase.kt                     | 100.0%   | 3/3                 | N/A                    |
| ✅ GetMyTrendingPodcastsUseCase.kt          | 100.0%   | 4/4                 | N/A                    |
| ✅ GetRecentPodcastsUseCase.kt              | 100.0%   | 11/11               | N/A                    |
| ✅ GetRecommendedPodcastsUseCase.kt         | 100.0%   | 5/5                 | N/A                    |
| ✅ GetPodcastsByChannelUseCase.kt           | 100.0%   | 3/3                 | N/A                    |
| ✅ ToggleFollowedUseCase.kt                 | 100.0%   | 3/3                 | N/A                    |
| ✅ GetMyRecentPodcastsUseCase.kt            | 100.0%   | 4/4                 | N/A                    |
| ✅ GetFollowedPodcastsUseCase.kt            | 100.0%   | 4/4                 | N/A                    |
| ✅ GetTrendingPodcastsUseCase.kt            | 100.0%   | 11/11               | N/A                    |

#### 📦 io.jacob.episodive.core.domain.usecase.episode - 78.0%

| Class                                    | Coverage | Lines Covered/Total | Branches Covered/Total |
|------------------------------------------|----------|---------------------|------------------------|
| ❌ IsLikedUseCase.kt                      | 0.0%     | 0/3                 | N/A                    |
| ❌ GetLikedEpisodesUseCase.kt             | 0.0%     | 0/4                 | N/A                    |
| ❌ GetAllPlayedEpisodesPagingUseCase.kt   | 0.0%     | 0/3                 | N/A                    |
| ❌ GetPlayingEpisodesPagingUseCase.kt     | 0.0%     | 0/3                 | N/A                    |
| ✅ GetEpisodesByPodcastIdPagingUseCase.kt | 100.0%   | 4/4                 | N/A                    |
| ✅ GetLikedEpisodesPagingUseCase.kt       | 100.0%   | 3/3                 | N/A                    |
| ✅ ToggleLikedUseCase.kt                  | 100.0%   | 3/3                 | N/A                    |
| ✅ GetAllPlayedEpisodesUseCase.kt         | 100.0%   | 4/4                 | N/A                    |
| ✅ GetLiveEpisodesUseCase.kt              | 100.0%   | 3/3                 | N/A                    |
| ✅ GetClipEpisodesPagingUseCase.kt        | 100.0%   | 4/4                 | N/A                    |
| ✅ UpdatePlayedEpisodeUseCase.kt          | 100.0%   | 11/11               | 2/4                    |
| ✅ GetMyRandomEpisodesUseCase.kt          | 100.0%   | 4/4                 | N/A                    |
| ✅ GetPlayingEpisodesUseCase.kt           | 100.0%   | 4/4                 | N/A                    |
| ✅ GetRecentEpisodesUseCase.kt            | 100.0%   | 3/3                 | N/A                    |
| ✅ GetChaptersUseCase.kt                  | 100.0%   | 3/3                 | N/A                    |

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
- **core:domain/GetChannelByIdUseCase.kt** (3 lines)
- **core:domain/IsLikedUseCase.kt** (3 lines)
- **core:domain/GetLikedEpisodesUseCase.kt** (4 lines)
- **core:domain/GetAllPlayedEpisodesPagingUseCase.kt** (3 lines)
- **core:domain/GetPlayingEpisodesPagingUseCase.kt** (3 lines)
- **core:domain/GetForeignTrendingPodcastsUseCase.kt** (5 lines)
- **core:domain/GetFollowedPodcastsPagingUseCase.kt** (4 lines)

### 🟡 Medium Priority: Low Coverage Classes (< 50%)

- **core:database/EpisodiveDatabase_AutoMigration_2_3_Impl.kt** - 16.7% (10 lines missed)
- **core:database/EpisodiveDatabase_AutoMigration_1_2_Impl.kt** - 33.3% (2 lines missed)
- **core:data/ConnectivityManagerNetworkMonitor.kt** - 45.5% (6 lines missed)

