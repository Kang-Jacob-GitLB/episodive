# Code Coverage Report

Generated: 2025-12-01 14:11:34

---

## Overall Summary

| Module         | Line Coverage  | Branch Coverage  | Total Lines   | Covered    | Missed     |
|----------------|----------------|------------------|---------------|------------|------------|
| core:data      | 86.8%          | 53.3%            | 552           | 479        | 73         |
| core:database  | 79.3%          | 47.1%            | 3918          | 3108       | 810        |
| core:datastore | 91.5%          | 65.0%            | 71            | 65         | 6          |
| core:domain    | 87.5%          | 75.0%            | 393           | 344        | 49         |
| core:network   | 88.5%          | 58.9%            | 468           | 414        | 54         |
| --------       | -------------- | ---------------- | ------------- | ---------  | --------   |
| **TOTAL**      | ** 81.6%**     | ** 49.9%**       | **  5402**    | **  4410** | **   992** |

---

## Classes with Low Coverage (< 80%)

| Module         | Package                        | Class                          | Coverage | Covered/Total | Missed |
|----------------|--------------------------------|--------------------------------|----------|---------------|--------|
| core:data      | io.jacob.episodive.core.data.r | ImageRepositoryImpl.kt         | 13.3%    | 2/15          | 13     |
| core:database  | io.jacob.episodive.core.databa | EpisodiveDatabase_AutoMigratio | 16.7%    | 2/12          | 10     |
| core:database  | io.jacob.episodive.core.databa | EpisodiveDatabase_AutoMigratio | 33.3%    | 1/3           | 2      |
| core:data      | io.jacob.episodive.core.data.u | ConnectivityManagerNetworkMoni | 45.5%    | 5/11          | 6      |
| core:domain    | io.jacob.episodive.core.domain | FlowExtKt.kt                   | 62.9%    | 22/35         | 13     |
| core:datastore | io.jacob.episodive.core.datast | UserPreferencesStore.kt        | 79.2%    | 19/24         | 5      |

---

## Detailed Module Reports

### core:data - 86.8% Coverage

#### 📦 io.jacob.episodive.core.data.util - 81.2%

| Class                                  | Coverage | Lines Covered/Total | Branches Covered/Total |
|----------------------------------------|----------|---------------------|------------------------|
| ❌ ConnectivityManagerNetworkMonitor.kt | 45.5%    | 5/11                | 0/4                    |
| ✅ Cacher.kt                            | 100.0%   | 4/4                 | N/A                    |
| ✅ ImageCacheInterceptor.kt             | 100.0%   | 17/17               | 4/4                    |

#### 📦 io.jacob.episodive.core.data.repository - 90.9%

| Class                           | Coverage | Lines Covered/Total | Branches Covered/Total |
|---------------------------------|----------|---------------------|------------------------|
| ❌ ImageRepositoryImpl.kt        | 13.3%    | 2/15                | 0/2                    |
| ⚠️ PodcastRepositoryImpl.kt     | 88.2%    | 30/34               | 0/8                    |
| ✅ UserRepositoryImpl.kt         | 90.9%    | 20/22               | 2/2                    |
| ✅ RecentSearchRepositoryImpl.kt | 92.3%    | 12/13               | N/A                    |
| ✅ EpisodeRepositoryImpl.kt      | 95.0%    | 76/80               | 0/20                   |
| ✅ PlayerRepositoryImpl.kt       | 100.0%   | 62/62               | N/A                    |
| ✅ FeedRepositoryImpl.kt         | 100.0%   | 39/39               | N/A                    |

#### 📦 io.jacob.episodive.core.data.util.updater - 100.0%

| Class                           | Coverage | Lines Covered/Total | Branches Covered/Total |
|---------------------------------|----------|---------------------|------------------------|
| ✅ SoundbiteRemoteUpdater.kt     | 100.0%   | 17/17               | 6/6                    |
| ✅ RecentFeedRemoteUpdater.kt    | 100.0%   | 20/20               | 6/6                    |
| ✅ RecentNewFeedRemoteUpdater.kt | 100.0%   | 17/17               | 6/6                    |
| ✅ EpisodeRemoteUpdater.kt       | 100.0%   | 33/33               | 21/22                  |
| ✅ PodcastRemoteUpdater.kt       | 100.0%   | 20/20               | 11/12                  |
| ✅ TrendingFeedRemoteUpdater.kt  | 100.0%   | 20/20               | 6/6                    |
| ✅ RemoteUpdater.kt              | 100.0%   | 11/11               | 2/2                    |

### core:database - 79.3% Coverage

#### 📦 io.jacob.episodive.core.database - 72.0%

| Class                                      | Coverage | Lines Covered/Total | Branches Covered/Total |
|--------------------------------------------|----------|---------------------|------------------------|
| ❌ EpisodiveDatabase_AutoMigration_2_3_Impl | 16.7%    | 2/12                | N/A                    |
| ❌ EpisodiveDatabase_AutoMigration_1_2_Impl | 33.3%    | 1/3                 | N/A                    |
| ✅ EpisodiveDatabase_Impl.kt                | 93.9%    | 31/33               | N/A                    |
| ✅ EpisodiveDatabase.kt                     | 100.0%   | 2/2                 | N/A                    |

#### 📦 io.jacob.episodive.core.database.dao - 89.5%

| Class                      | Coverage | Lines Covered/Total | Branches Covered/Total |
|----------------------------|----------|---------------------|------------------------|
| ⚠️ RecentSearchDao_Impl.kt | 83.0%    | 44/53               | 4/6                    |
| ⚠️ EpisodeDao_Impl.kt      | 87.6%    | 976/1114            | 150/296                |
| ✅ PodcastDao_Impl.kt       | 90.4%    | 766/847             | 107/204                |
| ✅ FeedDao_Impl.kt          | 93.3%    | 387/415             | 34/58                  |

#### 📦 io.jacob.episodive.core.database.mapper - 95.4%

| Class                 | Coverage | Lines Covered/Total | Branches Covered/Total |
|-----------------------|----------|---------------------|------------------------|
| ✅ DatabaseMapperKt.kt | 95.4%    | 332/348             | 3/6                    |

#### 📦 io.jacob.episodive.core.database.util - 100.0%

| Class                     | Coverage | Lines Covered/Total | Branches Covered/Total |
|---------------------------|----------|---------------------|------------------------|
| ✅ DurationConverter.kt    | 100.0%   | 3/3                 | 3/4                    |
| ✅ InstantConverter.kt     | 100.0%   | 3/3                 | 4/4                    |
| ✅ MediumConverter.kt      | 100.0%   | 3/3                 | 2/4                    |
| ✅ EpisodeTypeConverter.kt | 100.0%   | 3/3                 | 2/4                    |
| ✅ SoundbiteConverter.kt   | 100.0%   | 4/4                 | N/A                    |
| ✅ CategoryConverter.kt    | 100.0%   | 3/3                 | N/A                    |

#### 📦 io.jacob.episodive.core.database.datasource - 100.0%

| Class                                | Coverage | Lines Covered/Total | Branches Covered/Total |
|--------------------------------------|----------|---------------------|------------------------|
| ✅ PodcastLocalDataSourceImpl.kt      | 100.0%   | 24/24               | N/A                    |
| ✅ EpisodeLocalDataSourceImpl.kt      | 100.0%   | 31/31               | N/A                    |
| ✅ RecentSearchLocalDataSourceImpl.kt | 100.0%   | 9/9                 | N/A                    |
| ✅ FeedLocalDataSourceImpl.kt         | 100.0%   | 46/46               | N/A                    |

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

### core:domain - 87.5% Coverage

#### 📦 io.jacob.episodive.core.domain.util - 62.9%

| Class          | Coverage | Lines Covered/Total | Branches Covered/Total |
|----------------|----------|---------------------|------------------------|
| ❌ FlowExtKt.kt | 62.9%    | 22/35               | N/A                    |

#### 📦 io.jacob.episodive.core.domain.usecase.player - 97.4%

| Class                       | Coverage | Lines Covered/Total | Branches Covered/Total |
|-----------------------------|----------|---------------------|------------------------|
| ✅ PlayAndAddClipsUseCase.kt | 94.7%    | 18/19               | 7/10                   |
| ✅ PlayEpisodesUseCase.kt    | 100.0%   | 10/10               | 10/10                  |
| ✅ PlayEpisodeUseCase.kt     | 100.0%   | 4/4                 | N/A                    |
| ✅ ResumeEpisodeUseCase.kt   | 100.0%   | 5/5                 | N/A                    |

#### 📦 io.jacob.episodive.core.domain.usecase.episode - 100.0%

| Class                              | Coverage | Lines Covered/Total | Branches Covered/Total |
|------------------------------------|----------|---------------------|------------------------|
| ✅ GetEpisodesByPodcastIdUseCase.kt | 100.0%   | 5/5                 | N/A                    |
| ✅ ToggleLikedUseCase.kt            | 100.0%   | 3/3                 | N/A                    |
| ✅ GetClipEpisodesUseCase.kt        | 100.0%   | 5/5                 | N/A                    |
| ✅ GetLikedEpisodesUseCase.kt       | 100.0%   | 3/3                 | N/A                    |
| ✅ GetRecentEpisodesUseCase.kt      | 100.0%   | 3/3                 | N/A                    |
| ✅ GetAllPlayedEpisodesUseCase.kt   | 100.0%   | 3/3                 | N/A                    |
| ✅ GetLiveEpisodesUseCase.kt        | 100.0%   | 3/3                 | N/A                    |
| ✅ UpdatePlayedEpisodeUseCase.kt    | 100.0%   | 11/11               | 2/4                    |
| ✅ GetMyRandomEpisodesUseCase.kt    | 100.0%   | 4/4                 | N/A                    |
| ✅ GetPlayingEpisodesUseCase.kt     | 100.0%   | 3/3                 | N/A                    |
| ✅ GetChaptersUseCase.kt            | 100.0%   | 3/3                 | N/A                    |

#### 📦 io.jacob.episodive.core.domain.usecase.image - 100.0%

| Class                               | Coverage | Lines Covered/Total | Branches Covered/Total |
|-------------------------------------|----------|---------------------|------------------------|
| ✅ GetDominantColorFromUrlUseCase.kt | 100.0%   | 3/3                 | N/A                    |

#### 📦 io.jacob.episodive.core.domain.usecase.search - 100.0%

| Class                           | Coverage | Lines Covered/Total | Branches Covered/Total |
|---------------------------------|----------|---------------------|------------------------|
| ✅ UpsertRecentSearchUseCase.kt  | 100.0%   | 4/4                 | N/A                    |
| ✅ ClearRecentSearchesUseCase.kt | 100.0%   | 4/4                 | N/A                    |
| ✅ SearchUseCase.kt              | 100.0%   | 5/5                 | N/A                    |
| ✅ GetRecentSearchesUseCase.kt   | 100.0%   | 4/4                 | N/A                    |
| ✅ DeleteRecentSearchUseCase.kt  | 100.0%   | 4/4                 | N/A                    |

#### 📦 io.jacob.episodive.core.domain.usecase - 100.0%

| Class                     | Coverage | Lines Covered/Total | Branches Covered/Total |
|---------------------------|----------|---------------------|------------------------|
| ✅ GetSoundbitesUseCase.kt | 100.0%   | 3/3                 | N/A                    |
| ✅ FindInLibraryUseCase.kt | 100.0%   | 7/7                 | N/A                    |

#### 📦 io.jacob.episodive.core.domain.usecase.podcast - 100.0%

| Class                              | Coverage | Lines Covered/Total | Branches Covered/Total |
|------------------------------------|----------|---------------------|------------------------|
| ✅ GetMyTrendingPodcastsUseCase.kt  | 100.0%   | 4/4                 | N/A                    |
| ✅ GetRecommendedPodcastsUseCase.kt | 100.0%   | 5/5                 | N/A                    |
| ✅ ToggleFollowedUseCase.kt         | 100.0%   | 3/3                 | N/A                    |
| ✅ GetFollowedPodcastsUseCase.kt    | 100.0%   | 3/3                 | N/A                    |
| ✅ GetTrendingPodcastsUseCase.kt    | 100.0%   | 10/10               | N/A                    |
| ✅ GetPodcastUseCase.kt             | 100.0%   | 3/3                 | N/A                    |
| ✅ GetRecentPodcastsUseCase.kt      | 100.0%   | 10/10               | N/A                    |
| ✅ GetMyRecentPodcastsUseCase.kt    | 100.0%   | 4/4                 | N/A                    |

#### 📦 io.jacob.episodive.core.domain.usecase.user - 100.0%

| Class                               | Coverage | Lines Covered/Total | Branches Covered/Total |
|-------------------------------------|----------|---------------------|------------------------|
| ✅ ToggleCategoryUseCase.kt          | 100.0%   | 3/3                 | N/A                    |
| ✅ SetFirstLaunchOffUseCase.kt       | 100.0%   | 4/4                 | N/A                    |
| ✅ GetUserDataUseCase.kt             | 100.0%   | 3/3                 | N/A                    |
| ✅ SetSpeedUseCase.kt                | 100.0%   | 4/4                 | N/A                    |
| ✅ GetSelectableCategoriesUseCase.kt | 100.0%   | 4/4                 | N/A                    |
| ✅ GetPreferredCategoriesUseCase.kt  | 100.0%   | 3/3                 | N/A                    |

### core:network - 88.5% Coverage

#### 📦 io.jacob.episodive.core.network.util - 94.1%

| Class                       | Coverage | Lines Covered/Total | Branches Covered/Total |
|-----------------------------|----------|---------------------|------------------------|
| ✅ EpisodiveInterceptor.kt   | 92.9%    | 13/14               | N/A                    |
| ✅ EpisodiveInterceptorKt.kt | 100.0%   | 3/3                 | N/A                    |

#### 📦 io.jacob.episodive.core.network.mapper - 98.2%

| Class                | Coverage | Lines Covered/Total | Branches Covered/Total |
|----------------------|----------|---------------------|------------------------|
| ✅ NetworkMapperKt.kt | 98.2%    | 266/271             | 32/52                  |

#### 📦 io.jacob.episodive.core.network.datasource - 100.0%

| Class                            | Coverage | Lines Covered/Total | Branches Covered/Total |
|----------------------------------|----------|---------------------|------------------------|
| ✅ PodcastRemoteDataSourceImpl.kt | 100.0%   | 15/15               | N/A                    |
| ✅ ChapterRemoteDataSourceImpl.kt | 100.0%   | 9/9                 | N/A                    |
| ✅ FeedRemoteDataSourceImpl.kt    | 100.0%   | 29/29               | N/A                    |
| ✅ EpisodeRemoteDataSourceImpl.kt | 100.0%   | 39/39               | 1/4                    |

---

## Recommendations

### 🟡 Medium Priority: Low Coverage Classes (< 50%)

- **core:data/ImageRepositoryImpl.kt** - 13.3% (13 lines missed)
- **core:database/EpisodiveDatabase_AutoMigration_2_3_Impl.kt** - 16.7% (10 lines missed)
- **core:database/EpisodiveDatabase_AutoMigration_1_2_Impl.kt** - 33.3% (2 lines missed)
- **core:data/ConnectivityManagerNetworkMonitor.kt** - 45.5% (6 lines missed)

