# Code Coverage Report

Generated: 2025-12-24 15:14:59

---

## Overall Summary

| Module         | Line Coverage  | Branch Coverage  | Total Lines   | Covered    | Missed     |
|----------------|----------------|------------------|---------------|------------|------------|
| core:data      | 74.7%          | 41.7%            | 550           | 411        | 139        |
| core:database  | 84.9%          | 52.8%            | 5456          | 4632       | 824        |
| core:datastore | 91.5%          | 60.0%            | 71            | 65         | 6          |
| core:domain    | 85.1%          | 43.6%            | 296           | 252        | 44         |
| core:network   | 86.7%          | 58.1%            | 457           | 396        | 61         |
| --------       | -------------- | ---------------- | ------------- | ---------  | --------   |
| **TOTAL**      | ** 84.3%**     | ** 51.6%**       | **  6830**    | **  5756** | **  1074** |

---

## Classes with Low Coverage (< 80%)

| Module         | Package                        | Class                          | Coverage | Covered/Total | Missed |
|----------------|--------------------------------|--------------------------------|----------|---------------|--------|
| core:data      | io.jacob.episodive.core.data.r | PodcastRepositoryImpl.kt       | 44.7%    | 34/76         | 42     |
| core:data      | io.jacob.episodive.core.data.u | ConnectivityManagerNetworkMoni | 45.5%    | 5/11          | 6      |
| core:data      | io.jacob.episodive.core.data.r | EpisodeRepositoryImpl.kt       | 51.2%    | 62/121        | 59     |
| core:domain    | io.jacob.episodive.core.domain | GetLikedEpisodesUseCase.kt     | 75.0%    | 3/4           | 1      |
| core:datastore | io.jacob.episodive.core.datast | UserPreferencesStore.kt        | 79.2%    | 19/24         | 5      |

---

## Detailed Module Reports

### core:data - 74.7% Coverage

#### 📦 io.jacob.episodive.core.data.repository - 64.6%

| Class                           | Coverage | Lines Covered/Total | Branches Covered/Total |
|---------------------------------|----------|---------------------|------------------------|
| ❌ PodcastRepositoryImpl.kt      | 44.7%    | 34/76               | N/A                    |
| ❌ EpisodeRepositoryImpl.kt      | 51.2%    | 62/121              | N/A                    |
| ✅ UserRepositoryImpl.kt         | 90.9%    | 20/22               | 2/2                    |
| ✅ RecentSearchRepositoryImpl.kt | 92.3%    | 12/13               | N/A                    |
| ✅ PlayerRepositoryImpl.kt       | 98.4%    | 60/61               | N/A                    |
| ✅ ChannelRepositoryImpl.kt      | 100.0%   | 4/4                 | N/A                    |

#### 📦 io.jacob.episodive.core.data.util - 78.6%

| Class                                  | Coverage | Lines Covered/Total | Branches Covered/Total |
|----------------------------------------|----------|---------------------|------------------------|
| ❌ ConnectivityManagerNetworkMonitor.kt | 45.5%    | 5/11                | 0/4                    |
| ✅ ImageCacheInterceptor.kt             | 100.0%   | 17/17               | 4/4                    |

#### 📦 io.jacob.episodive.core.data.util.updater - 95.2%

| Class                     | Coverage | Lines Covered/Total | Branches Covered/Total |
|---------------------------|----------|---------------------|------------------------|
| ✅ PodcastRemoteUpdater.kt | 93.1%    | 27/29               | 21/30                  |
| ✅ EpisodeRemoteUpdater.kt | 94.6%    | 35/37               | 15/18                  |
| ✅ RemoteUpdater.kt        | 100.0%   | 18/18               | 1/2                    |

### core:database - 84.9% Coverage

#### 📦 io.jacob.episodive.core.database.dao - 90.5%

| Class                      | Coverage | Lines Covered/Total | Branches Covered/Total |
|----------------------------|----------|---------------------|------------------------|
| ⚠️ RecentSearchDao_Impl.kt | 83.3%    | 45/54               | 4/6                    |
| ⚠️ EpisodeDao_Impl.kt      | 89.6%    | 1364/1523           | 233/416                |
| ✅ PodcastDao_Impl.kt       | 91.6%    | 1033/1128           | 148/264                |
| ✅ SoundbiteDao_Impl.kt     | 92.0%    | 104/113             | 10/18                  |
| ✅ EpisodeDao.kt            | 94.9%    | 37/39               | 2/2                    |
| ✅ PodcastDao.kt            | 96.8%    | 30/31               | 2/2                    |
| ✅ SoundbiteDao.kt          | 100.0%   | 3/3                 | N/A                    |

#### 📦 io.jacob.episodive.core.database - 95.9%

| Class                       | Coverage | Lines Covered/Total | Branches Covered/Total |
|-----------------------------|----------|---------------------|------------------------|
| ✅ EpisodiveDatabase_Impl.kt | 95.8%    | 46/48               | N/A                    |
| ✅ EpisodiveDatabase.kt      | 100.0%   | 1/1                 | N/A                    |

#### 📦 io.jacob.episodive.core.database.datasource - 97.7%

| Class                                | Coverage | Lines Covered/Total | Branches Covered/Total |
|--------------------------------------|----------|---------------------|------------------------|
| ✅ EpisodeLocalDataSourceImpl.kt      | 95.0%    | 38/40               | 7/12                   |
| ✅ PodcastLocalDataSourceImpl.kt      | 100.0%   | 25/25               | 5/8                    |
| ✅ RecentSearchLocalDataSourceImpl.kt | 100.0%   | 9/9                 | N/A                    |
| ✅ SoundbiteLocalDataSourceImpl.kt    | 100.0%   | 13/13               | N/A                    |

#### 📦 io.jacob.episodive.core.database.mapper - 99.0%

| Class                 | Coverage | Lines Covered/Total | Branches Covered/Total |
|-----------------------|----------|---------------------|------------------------|
| ✅ DatabaseMapperKt.kt | 99.0%    | 191/193             | 3/6                    |

#### 📦 io.jacob.episodive.core.database.util - 100.0%

| Class                     | Coverage | Lines Covered/Total | Branches Covered/Total |
|---------------------------|----------|---------------------|------------------------|
| ✅ FtsExtKt.kt             | 100.0%   | 3/3                 | 6/6                    |
| ✅ DurationConverter.kt    | 100.0%   | 3/3                 | 3/4                    |
| ✅ InstantConverter.kt     | 100.0%   | 3/3                 | 4/4                    |
| ✅ MediumConverter.kt      | 100.0%   | 3/3                 | 2/4                    |
| ✅ EpisodeTypeConverter.kt | 100.0%   | 3/3                 | 2/4                    |
| ✅ CategoryConverter.kt    | 100.0%   | 3/3                 | N/A                    |

#### 📦 io.jacob.episodive.core.database.migration - 100.0%

| Class                  | Coverage | Lines Covered/Total | Branches Covered/Total |
|------------------------|----------|---------------------|------------------------|
| ✅ AutoMigration4to5.kt | 100.0%   | 1/1                 | N/A                    |
| ✅ AutoMigration3to4.kt | 100.0%   | 1/1                 | N/A                    |
| ✅ AutoMigration6to7.kt | 100.0%   | 1/1                 | N/A                    |
| ✅ AutoMigration2to3.kt | 100.0%   | 1/1                 | N/A                    |
| ✅ AutoMigration5to6.kt | 100.0%   | 1/1                 | N/A                    |

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

### core:domain - 85.1% Coverage

#### 📦 io.jacob.episodive.core.domain.usecase.episode - 98.5%

| Class                                    | Coverage | Lines Covered/Total | Branches Covered/Total |
|------------------------------------------|----------|---------------------|------------------------|
| ❌ GetLikedEpisodesUseCase.kt             | 75.0%    | 3/4                 | N/A                    |
| ✅ ToggleLikedEpisodeUseCase.kt           | 100.0%   | 4/4                 | N/A                    |
| ✅ GetEpisodesByPodcastIdPagingUseCase.kt | 100.0%   | 4/4                 | N/A                    |
| ✅ GetLikedEpisodesPagingUseCase.kt       | 100.0%   | 3/3                 | N/A                    |
| ✅ GetAllPlayedEpisodesUseCase.kt         | 100.0%   | 8/8                 | N/A                    |
| ✅ GetLiveEpisodesUseCase.kt              | 100.0%   | 3/3                 | N/A                    |
| ✅ GetClipEpisodesPagingUseCase.kt        | 100.0%   | 3/3                 | N/A                    |
| ✅ IsLikedEpisodeUseCase.kt               | 100.0%   | 3/3                 | N/A                    |
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
| ✅ SearchUseCase.kt              | 100.0%   | 4/4                 | N/A                    |
| ✅ GetRecentSearchesUseCase.kt   | 100.0%   | 3/3                 | N/A                    |
| ✅ DeleteRecentSearchUseCase.kt  | 100.0%   | 4/4                 | N/A                    |
| ✅ ClearRecentSearchesUseCase.kt | 100.0%   | 4/4                 | N/A                    |

#### 📦 io.jacob.episodive.core.domain.usecase - 100.0%

| Class                     | Coverage | Lines Covered/Total | Branches Covered/Total |
|---------------------------|----------|---------------------|------------------------|
| ✅ FindInLibraryUseCase.kt | 100.0%   | 7/7                 | N/A                    |

#### 📦 io.jacob.episodive.core.domain.usecase.podcast - 100.0%

| Class                                  | Coverage | Lines Covered/Total | Branches Covered/Total |
|----------------------------------------|----------|---------------------|------------------------|
| ✅ GetForeignTrendingPodcastsUseCase.kt | 100.0%   | 5/5                 | N/A                    |
| ✅ GetPodcastUseCase.kt                 | 100.0%   | 3/3                 | N/A                    |
| ✅ GetMyTrendingPodcastsUseCase.kt      | 100.0%   | 4/4                 | N/A                    |
| ✅ GetRecentPodcastsUseCase.kt          | 100.0%   | 6/6                 | N/A                    |
| ✅ GetRecommendedPodcastsUseCase.kt     | 100.0%   | 6/6                 | N/A                    |
| ✅ GetFollowedPodcastsPagingUseCase.kt  | 100.0%   | 4/4                 | N/A                    |
| ✅ GetPodcastsByChannelUseCase.kt       | 100.0%   | 3/3                 | N/A                    |
| ✅ GetLocalTrendingPodcastsUseCase.kt   | 100.0%   | 4/4                 | N/A                    |
| ✅ ToggleFollowedUseCase.kt             | 100.0%   | 3/3                 | N/A                    |
| ✅ GetMyRecentPodcastsUseCase.kt        | 100.0%   | 4/4                 | N/A                    |
| ✅ GetFollowedPodcastsUseCase.kt        | 100.0%   | 4/4                 | N/A                    |
| ✅ GetTrendingPodcastsUseCase.kt        | 100.0%   | 6/6                 | N/A                    |

#### 📦 io.jacob.episodive.core.domain.usecase.player - 100.0%

| Class                     | Coverage | Lines Covered/Total | Branches Covered/Total |
|---------------------------|----------|---------------------|------------------------|
| ✅ GetNowPlayingUseCase.kt | 100.0%   | 4/4                 | N/A                    |
| ✅ PlayEpisodeUseCase.kt   | 100.0%   | 19/19               | 10/10                  |
| ✅ ResumeEpisodeUseCase.kt | 100.0%   | 5/5                 | N/A                    |
| ✅ GetPlaylistUseCase.kt   | 100.0%   | 4/4                 | N/A                    |

#### 📦 io.jacob.episodive.core.domain.usecase.user - 100.0%

| Class                               | Coverage | Lines Covered/Total | Branches Covered/Total |
|-------------------------------------|----------|---------------------|------------------------|
| ✅ ToggleCategoryUseCase.kt          | 100.0%   | 3/3                 | N/A                    |
| ✅ SetFirstLaunchOffUseCase.kt       | 100.0%   | 4/4                 | N/A                    |
| ✅ GetUserDataUseCase.kt             | 100.0%   | 3/3                 | N/A                    |
| ✅ SetSpeedUseCase.kt                | 100.0%   | 4/4                 | N/A                    |
| ✅ GetSelectableCategoriesUseCase.kt | 100.0%   | 4/4                 | N/A                    |
| ✅ GetPreferredCategoriesUseCase.kt  | 100.0%   | 3/3                 | N/A                    |

### core:network - 86.7% Coverage

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

| Class                              | Coverage | Lines Covered/Total | Branches Covered/Total |
|------------------------------------|----------|---------------------|------------------------|
| ✅ PodcastRemoteDataSourceImpl.kt   | 100.0%   | 16/16               | N/A                    |
| ✅ SoundbiteRemoteDataSourceImpl.kt | 100.0%   | 3/3                 | N/A                    |
| ✅ ChapterRemoteDataSourceImpl.kt   | 100.0%   | 9/9                 | N/A                    |
| ✅ FeedRemoteDataSourceImpl.kt      | 100.0%   | 28/28               | N/A                    |
| ✅ ChannelRemoteDataSourceImpl.kt   | 100.0%   | 7/7                 | 3/6                    |
| ✅ EpisodeRemoteDataSourceImpl.kt   | 100.0%   | 39/39               | 1/4                    |

---

## Recommendations

### 🟡 Medium Priority: Low Coverage Classes (< 50%)

- **core:data/PodcastRepositoryImpl.kt** - 44.7% (42 lines missed)
- **core:data/ConnectivityManagerNetworkMonitor.kt** - 45.5% (6 lines missed)

