# Code Coverage Report

Generated: 2026-03-16 13:08:08

---

## Overall Summary

| Module | Line Coverage | Branch Coverage | Total Lines | Covered | Missed |
|--------|--------------|----------------|-------------|---------|--------|
| core:data       |  51.2% |  22.5% |    778 |    398 |    380 |
| core:database   |  77.9% |  49.4% |   6559 |   5111 |   1448 |
| core:datastore  |  71.8% |  13.3% |     71 |     51 |     20 |
| core:domain     |  86.4% |  45.3% |    287 |    248 |     39 |
| core:network    |  87.2% |  58.1% |    477 |    416 |     61 |
| feature:channel |  20.8% |   5.8% |    240 |     50 |    190 |
| feature:clip    |  27.9% |   5.6% |    233 |     65 |    168 |
| feature:home    |  28.3% |   7.4% |    307 |     87 |    220 |
| feature:library |  17.7% |   4.0% |    745 |    132 |    613 |
| feature:onboarding |  20.8% |   8.1% |    409 |     85 |    324 |
| feature:player  |  16.9% |   6.8% |   1013 |    171 |    842 |
| feature:podcast |  24.9% |   9.3% |    229 |     57 |    172 |
| feature:search  |  25.4% |   5.4% |    437 |    111 |    326 |
|--------|--------------|----------------|-------------|---------|--------|
| **TOTAL** | ** 59.2%** | ** 22.3%** | ** 11785** | **  6982** | **  4803** |

---

## Classes with Low Coverage (< 80%)

| Module | Package | Class | Coverage | Covered/Total | Missed |
|--------|---------|-------|----------|---------------|--------|
| core:data  | io.jacob.episodive.core.data.u | SoundbiteEpisodePagingSource.k |   0.0% | 0/84 | 84 |
| core:data  | io.jacob.episodive.core.data.u | RecommendedPodcastPagingSource |   0.0% | 0/70 | 70 |
| feature:home | io.jacob.episodive.feature.hom | HomeScreenKt.kt                |   0.0% | 0/151 | 151 |
| feature:home | io.jacob.episodive.feature.hom | HomeNavigationKt.kt            |   0.0% | 0/31 | 31 |
| feature:home | io.jacob.episodive.feature.hom | HomeBaseRoute.kt               |   0.0% | 0/2 | 2 |
| feature:home | io.jacob.episodive.feature.hom | HomeRoute.kt                   |   0.0% | 0/2 | 2 |
| feature:search | io.jacob.episodive.feature.sea | SearchBaseRoute.kt             |   0.0% | 0/2 | 2 |
| feature:search | io.jacob.episodive.feature.sea | SearchRoute.kt                 |   0.0% | 0/2 | 2 |
| feature:search | io.jacob.episodive.feature.sea | SearchNavigationKt.kt          |   0.0% | 0/28 | 28 |
| feature:search | io.jacob.episodive.feature.sea | SearchScreenKt.kt              |   0.0% | 0/228 | 228 |
| feature:library | io.jacob.episodive.feature.lib | LibraryScreenKt.kt             |   0.0% | 0/491 | 491 |
| feature:library | io.jacob.episodive.feature.lib | LibraryBaseRoute.kt            |   0.0% | 0/2 | 2 |
| feature:library | io.jacob.episodive.feature.lib | LibraryRoute.kt                |   0.0% | 0/2 | 2 |
| feature:library | io.jacob.episodive.feature.lib | LibraryNavigationKt.kt         |   0.0% | 0/28 | 28 |
| feature:podcast | io.jacob.episodive.feature.pod | PodcastRoute.kt                |   0.0% | 0/2 | 2 |
| feature:podcast | io.jacob.episodive.feature.pod | PodcastNavigationKt.kt         |   0.0% | 0/14 | 14 |
| feature:podcast | io.jacob.episodive.feature.pod | PodcastScreenKt.kt             |   0.0% | 0/134 | 134 |
| feature:player | io.jacob.episodive.feature.pla | PlayerScreenKt.kt              |   0.0% | 0/551 | 551 |
| feature:player | io.jacob.episodive.feature.pla | PlayerBarKt.kt                 |   0.0% | 0/119 | 119 |
| feature:clip | io.jacob.episodive.feature.cli | ClipScreenKt.kt                |   0.0% | 0/92 | 92 |
| feature:clip | io.jacob.episodive.feature.cli | ClipRoute.kt                   |   0.0% | 0/2 | 2 |
| feature:clip | io.jacob.episodive.feature.cli | ClipBaseRoute.kt               |   0.0% | 0/2 | 2 |
| feature:clip | io.jacob.episodive.feature.cli | ClipNavigationKt.kt            |   0.0% | 0/28 | 28 |
| feature:onboarding | io.jacob.episodive.feature.onb | OnboardingScreenKt.kt          |   0.0% | 0/246 | 246 |
| feature:channel | io.jacob.episodive.feature.cha | ChannelScreenKt.kt             |   0.0% | 0/139 | 139 |
| feature:channel | io.jacob.episodive.feature.cha | ChannelNavigationKt.kt         |   0.0% | 0/15 | 15 |
| feature:channel | io.jacob.episodive.feature.cha | ChannelRoute.kt                |   0.0% | 0/2 | 2 |
| core:data  | io.jacob.episodive.core.data.r | PodcastRepositoryImpl.kt       |  42.4% | 36/85 | 49 |
| core:data  | io.jacob.episodive.core.data.u | ConnectivityManagerNetworkMoni |  45.5% | 5/11 | 6 |
| core:data  | io.jacob.episodive.core.data.r | EpisodeRepositoryImpl.kt       |  55.7% | 64/115 | 51 |
| core:database | io.jacob.episodive.core.databa | SoundbiteDao_Impl.kt           |  57.8% | 104/180 | 76 |
| core:datastore | io.jacob.episodive.core.datast | UserPreferencesStore.kt        |  70.8% | 17/24 | 7 |
| core:domain | io.jacob.episodive.core.domain | GetLikedEpisodesUseCase.kt     |  75.0% | 3/4 | 1 |
| core:database | io.jacob.episodive.core.databa | EpisodeDao_Impl.kt             |  79.7% | 1463/1835 | 372 |
| core:database | io.jacob.episodive.core.databa | PodcastDao_Impl.kt             |  79.8% | 1120/1404 | 284 |

---

## Detailed Module Reports

### core:data - 51.2% Coverage

#### 📦 io.jacob.episodive.core.data.util.paging - 0.0%

| Class | Coverage | Lines Covered/Total | Branches Covered/Total |
|-------|----------|---------------------|------------------------|
| ❌ SoundbiteEpisodePagingSource.kt          |   0.0% | 0/84 | 0/44 |
| ❌ RecommendedPodcastPagingSource.kt        |   0.0% | 0/70 | 0/34 |

#### 📦 io.jacob.episodive.core.data.repository - 65.3%

| Class | Coverage | Lines Covered/Total | Branches Covered/Total |
|-------|----------|---------------------|------------------------|
| ❌ PodcastRepositoryImpl.kt                 |  42.4% | 36/85 | N/A |
| ❌ EpisodeRepositoryImpl.kt                 |  55.7% | 64/115 | N/A |
| ✅ UserRepositoryImpl.kt                    |  90.9% | 20/22 | 2/2 |
| ✅ RecentSearchRepositoryImpl.kt            |  92.3% | 12/13 | N/A |
| ✅ PlayerRepositoryImpl.kt                  |  98.4% | 60/61 | N/A |
| ✅ ChannelRepositoryImpl.kt                 | 100.0% | 4/4 | N/A |

#### 📦 io.jacob.episodive.core.data.util - 78.6%

| Class | Coverage | Lines Covered/Total | Branches Covered/Total |
|-------|----------|---------------------|------------------------|
| ❌ ConnectivityManagerNetworkMonitor.kt     |  45.5% | 5/11 | 0/4 |
| ✅ ImageCacheInterceptor.kt                 | 100.0% | 17/17 | 4/4 |

#### 📦 io.jacob.episodive.core.data.util.updater - 95.2%

| Class | Coverage | Lines Covered/Total | Branches Covered/Total |
|-------|----------|---------------------|------------------------|
| ✅ PodcastRemoteUpdater.kt                  |  94.1% | 48/51 | 22/32 |
| ✅ EpisodeRemoteUpdater.kt                  |  94.4% | 34/36 | 13/16 |
| ✅ RemoteUpdater.kt                         | 100.0% | 18/18 | 1/2 |

### core:database - 77.9% Coverage

#### 📦 io.jacob.episodive.core.database.dao - 79.7%

| Class | Coverage | Lines Covered/Total | Branches Covered/Total |
|-------|----------|---------------------|------------------------|
| ❌ SoundbiteDao_Impl.kt                     |  57.8% | 104/180 | 10/32 |
| ❌ EpisodeDao_Impl.kt                       |  79.7% | 1463/1835 | 251/496 |
| ❌ PodcastDao_Impl.kt                       |  79.8% | 1120/1404 | 165/330 |
| ⚠️ RecentSearchDao_Impl.kt                  |  83.3% | 45/54 | 4/6 |
| ⚠️ FeedDao_Impl.kt                          |  88.8% | 175/197 | 21/36 |
| ✅ EpisodeDao.kt                            |  95.2% | 59/62 | 9/12 |
| ✅ PodcastDao.kt                            |  96.2% | 50/52 | 8/10 |
| ✅ FeedDao.kt                               | 100.0% | 3/3 | N/A |
| ✅ SoundbiteDao.kt                          | 100.0% | 3/3 | N/A |

#### 📦 io.jacob.episodive.core.database.mapper - 88.4%

| Class | Coverage | Lines Covered/Total | Branches Covered/Total |
|-------|----------|---------------------|------------------------|
| ⚠️ DatabaseMapperKt.kt                      |  88.4% | 191/216 | 3/6 |

#### 📦 io.jacob.episodive.core.database.datasource - 90.0%

| Class | Coverage | Lines Covered/Total | Branches Covered/Total |
|-------|----------|---------------------|------------------------|
| ⚠️ PodcastLocalDataSourceImpl.kt            |  83.9% | 26/31 | 5/8 |
| ⚠️ EpisodeLocalDataSourceImpl.kt            |  88.6% | 39/44 | 7/12 |
| ✅ SoundbiteLocalDataSourceImpl.kt          |  91.7% | 11/12 | N/A |
| ✅ RecentSearchLocalDataSourceImpl.kt       | 100.0% | 9/9 | N/A |
| ✅ FeedLocalDataSourceImpl.kt               | 100.0% | 14/14 | N/A |

#### 📦 io.jacob.episodive.core.database - 96.3%

| Class | Coverage | Lines Covered/Total | Branches Covered/Total |
|-------|----------|---------------------|------------------------|
| ✅ EpisodiveDatabase_Impl.kt                |  96.2% | 51/53 | N/A |
| ✅ EpisodiveDatabase.kt                     | 100.0% | 1/1 | N/A |

#### 📦 io.jacob.episodive.core.database.util - 100.0%

| Class | Coverage | Lines Covered/Total | Branches Covered/Total |
|-------|----------|---------------------|------------------------|
| ✅ FtsExtKt.kt                              | 100.0% | 3/3 | 6/6 |
| ✅ DurationConverter.kt                     | 100.0% | 3/3 | 3/4 |
| ✅ InstantConverter.kt                      | 100.0% | 3/3 | 4/4 |
| ✅ MediumConverter.kt                       | 100.0% | 3/3 | 2/4 |
| ✅ EpisodeTypeConverter.kt                  | 100.0% | 3/3 | 2/4 |
| ✅ CategoryConverter.kt                     | 100.0% | 3/3 | N/A |

#### 📦 io.jacob.episodive.core.database.migration - 100.0%

| Class | Coverage | Lines Covered/Total | Branches Covered/Total |
|-------|----------|---------------------|------------------------|
| ✅ AutoMigration4to5.kt                     | 100.0% | 1/1 | N/A |
| ✅ AutoMigration3to4.kt                     | 100.0% | 1/1 | N/A |
| ✅ AutoMigration6to7.kt                     | 100.0% | 1/1 | N/A |
| ✅ AutoMigration2to3.kt                     | 100.0% | 1/1 | N/A |
| ✅ AutoMigration5to6.kt                     | 100.0% | 1/1 | N/A |

### core:datastore - 71.8% Coverage

#### 📦 io.jacob.episodive.core.datastore.store - 70.8%

| Class | Coverage | Lines Covered/Total | Branches Covered/Total |
|-------|----------|---------------------|------------------------|
| ❌ UserPreferencesStore.kt                  |  70.8% | 17/24 | N/A |

#### 📦 io.jacob.episodive.core.datastore.datasource - 100.0%

| Class | Coverage | Lines Covered/Total | Branches Covered/Total |
|-------|----------|---------------------|------------------------|
| ✅ UserPreferencesDataSourceImpl.kt         | 100.0% | 14/14 | N/A |

#### 📦 io.jacob.episodive.core.datastore.mapper - 100.0%

| Class | Coverage | Lines Covered/Total | Branches Covered/Total |
|-------|----------|---------------------|------------------------|
| ✅ DataStoreMapperKt.kt                     | 100.0% | 12/12 | N/A |

### core:domain - 86.4% Coverage

#### 📦 io.jacob.episodive.core.domain.usecase.episode - 98.5%

| Class | Coverage | Lines Covered/Total | Branches Covered/Total |
|-------|----------|---------------------|------------------------|
| ❌ GetLikedEpisodesUseCase.kt               |  75.0% | 3/4 | N/A |
| ✅ ToggleLikedEpisodeUseCase.kt             | 100.0% | 4/4 | N/A |
| ✅ GetEpisodesByPodcastIdPagingUseCase.kt   | 100.0% | 4/4 | N/A |
| ✅ GetLikedEpisodesPagingUseCase.kt         | 100.0% | 3/3 | N/A |
| ✅ GetAllPlayedEpisodesUseCase.kt           | 100.0% | 8/8 | N/A |
| ✅ GetLiveEpisodesUseCase.kt                | 100.0% | 3/3 | N/A |
| ✅ GetClipEpisodesPagingUseCase.kt          | 100.0% | 3/3 | N/A |
| ✅ GetAllPlayedEpisodesPagingUseCase.kt     | 100.0% | 7/7 | N/A |
| ✅ UpdatePlayedEpisodeUseCase.kt            | 100.0% | 11/11 | 2/4 |
| ✅ GetMyRandomEpisodesUseCase.kt            | 100.0% | 4/4 | N/A |
| ✅ GetPlayingEpisodesUseCase.kt             | 100.0% | 8/8 | N/A |
| ✅ GetRecentEpisodesUseCase.kt              | 100.0% | 3/3 | N/A |
| ✅ GetChaptersUseCase.kt                    | 100.0% | 3/3 | N/A |

#### 📦 io.jacob.episodive.core.domain.usecase.channel - 100.0%

| Class | Coverage | Lines Covered/Total | Branches Covered/Total |
|-------|----------|---------------------|------------------------|
| ✅ GetChannelByIdUseCase.kt                 | 100.0% | 3/3 | N/A |
| ✅ GetChannelsUseCase.kt                    | 100.0% | 3/3 | N/A |

#### 📦 io.jacob.episodive.core.domain.usecase.search - 100.0%

| Class | Coverage | Lines Covered/Total | Branches Covered/Total |
|-------|----------|---------------------|------------------------|
| ✅ UpsertRecentSearchUseCase.kt             | 100.0% | 4/4 | N/A |
| ✅ SearchUseCase.kt                         | 100.0% | 4/4 | N/A |
| ✅ GetRecentSearchesUseCase.kt              | 100.0% | 3/3 | N/A |
| ✅ DeleteRecentSearchUseCase.kt             | 100.0% | 4/4 | N/A |
| ✅ ClearRecentSearchesUseCase.kt            | 100.0% | 4/4 | N/A |

#### 📦 io.jacob.episodive.core.domain.usecase - 100.0%

| Class | Coverage | Lines Covered/Total | Branches Covered/Total |
|-------|----------|---------------------|------------------------|
| ✅ FindInLibraryUseCase.kt                  | 100.0% | 7/7 | N/A |

#### 📦 io.jacob.episodive.core.domain.usecase.podcast - 100.0%

| Class | Coverage | Lines Covered/Total | Branches Covered/Total |
|-------|----------|---------------------|------------------------|
| ✅ GetForeignTrendingPodcastsUseCase.kt     | 100.0% | 5/5 | N/A |
| ✅ GetUserRecentPodcastsUseCase.kt          | 100.0% | 4/4 | N/A |
| ✅ GetPodcastUseCase.kt                     | 100.0% | 3/3 | N/A |
| ✅ GetRecentPodcastsUseCase.kt              | 100.0% | 6/6 | N/A |
| ✅ GetUserRecommendedPodcastsPagingUseCase. | 100.0% | 4/4 | N/A |
| ✅ GetFollowedPodcastsPagingUseCase.kt      | 100.0% | 4/4 | N/A |
| ✅ GetPodcastsByChannelUseCase.kt           | 100.0% | 3/3 | N/A |
| ✅ GetLocalTrendingPodcastsUseCase.kt       | 100.0% | 4/4 | N/A |
| ✅ GetUserTrendingPodcastsUseCase.kt        | 100.0% | 4/4 | N/A |
| ✅ ToggleFollowedUseCase.kt                 | 100.0% | 3/3 | N/A |
| ✅ GetFollowedPodcastsUseCase.kt            | 100.0% | 4/4 | N/A |
| ✅ GetTrendingPodcastsUseCase.kt            | 100.0% | 6/6 | N/A |

#### 📦 io.jacob.episodive.core.domain.usecase.player - 100.0%

| Class | Coverage | Lines Covered/Total | Branches Covered/Total |
|-------|----------|---------------------|------------------------|
| ✅ GetNowPlayingUseCase.kt                  | 100.0% | 4/4 | N/A |
| ✅ PlayEpisodeUseCase.kt                    | 100.0% | 19/19 | 10/10 |
| ✅ ResumeEpisodeUseCase.kt                  | 100.0% | 5/5 | N/A |
| ✅ GetPlaylistUseCase.kt                    | 100.0% | 4/4 | N/A |

#### 📦 io.jacob.episodive.core.domain.usecase.user - 100.0%

| Class | Coverage | Lines Covered/Total | Branches Covered/Total |
|-------|----------|---------------------|------------------------|
| ✅ ToggleCategoryUseCase.kt                 | 100.0% | 3/3 | N/A |
| ✅ SetFirstLaunchOffUseCase.kt              | 100.0% | 4/4 | N/A |
| ✅ GetUserDataUseCase.kt                    | 100.0% | 3/3 | N/A |
| ✅ SetSpeedUseCase.kt                       | 100.0% | 4/4 | N/A |
| ✅ GetSelectableCategoriesUseCase.kt        | 100.0% | 4/4 | N/A |
| ✅ GetPreferredCategoriesUseCase.kt         | 100.0% | 3/3 | N/A |

### core:network - 87.2% Coverage

#### 📦 io.jacob.episodive.core.network.util - 94.1%

| Class | Coverage | Lines Covered/Total | Branches Covered/Total |
|-------|----------|---------------------|------------------------|
| ✅ EpisodiveInterceptor.kt                  |  92.9% | 13/14 | N/A |
| ✅ EpisodiveInterceptorKt.kt                | 100.0% | 3/3 | N/A |

#### 📦 io.jacob.episodive.core.network.mapper - 97.9%

| Class | Coverage | Lines Covered/Total | Branches Covered/Total |
|-------|----------|---------------------|------------------------|
| ✅ NetworkMapperKt.kt                       |  97.9% | 275/281 | 32/52 |

#### 📦 io.jacob.episodive.core.network.datasource - 100.0%

| Class | Coverage | Lines Covered/Total | Branches Covered/Total |
|-------|----------|---------------------|------------------------|
| ✅ PodcastRemoteDataSourceImpl.kt           | 100.0% | 22/22 | N/A |
| ✅ SoundbiteRemoteDataSourceImpl.kt         | 100.0% | 4/4 | N/A |
| ✅ ChapterRemoteDataSourceImpl.kt           | 100.0% | 10/10 | N/A |
| ✅ FeedRemoteDataSourceImpl.kt              | 100.0% | 32/32 | N/A |
| ✅ ChannelRemoteDataSourceImpl.kt           | 100.0% | 7/7 | 3/6 |
| ✅ EpisodeRemoteDataSourceImpl.kt           | 100.0% | 47/47 | 1/4 |

### feature:channel - 20.8% Coverage

#### 📦 io.jacob.episodive.feature.channel.navigation - 0.0%

| Class | Coverage | Lines Covered/Total | Branches Covered/Total |
|-------|----------|---------------------|------------------------|
| ❌ ChannelNavigationKt.kt                   |   0.0% | 0/15 | N/A |
| ❌ ChannelRoute.kt                          |   0.0% | 0/2 | 0/2 |

#### 📦 io.jacob.episodive.feature.channel - 16.8%

| Class | Coverage | Lines Covered/Total | Branches Covered/Total |
|-------|----------|---------------------|------------------------|
| ❌ ChannelScreenKt.kt                       |   0.0% | 0/139 | 0/118 |
| ✅ ChannelViewModel.kt                      | 100.0% | 28/28 | N/A |

### feature:clip - 27.9% Coverage

#### 📦 io.jacob.episodive.feature.clip.navigation - 0.0%

| Class | Coverage | Lines Covered/Total | Branches Covered/Total |
|-------|----------|---------------------|------------------------|
| ❌ ClipRoute.kt                             |   0.0% | 0/2 | N/A |
| ❌ ClipBaseRoute.kt                         |   0.0% | 0/2 | N/A |
| ❌ ClipNavigationKt.kt                      |   0.0% | 0/28 | 0/24 |

#### 📦 io.jacob.episodive.feature.clip - 30.8%

| Class | Coverage | Lines Covered/Total | Branches Covered/Total |
|-------|----------|---------------------|------------------------|
| ❌ ClipScreenKt.kt                          |   0.0% | 0/92 | 0/181 |
| ✅ ClipPlayerState.kt                       | 100.0% | 5/5 | N/A |
| ✅ ClipViewModel.kt                         | 100.0% | 36/36 | N/A |

### feature:home - 28.3% Coverage

#### 📦 io.jacob.episodive.feature.home.navigation - 0.0%

| Class | Coverage | Lines Covered/Total | Branches Covered/Total |
|-------|----------|---------------------|------------------------|
| ❌ HomeNavigationKt.kt                      |   0.0% | 0/31 | 0/30 |
| ❌ HomeBaseRoute.kt                         |   0.0% | 0/2 | N/A |
| ❌ HomeRoute.kt                             |   0.0% | 0/2 | N/A |

#### 📦 io.jacob.episodive.feature.home - 21.4%

| Class | Coverage | Lines Covered/Total | Branches Covered/Total |
|-------|----------|---------------------|------------------------|
| ❌ HomeScreenKt.kt                          |   0.0% | 0/151 | 0/122 |
| ✅ HomeViewModel.kt                         | 100.0% | 41/41 | N/A |

### feature:library - 17.7% Coverage

#### 📦 io.jacob.episodive.feature.library.navigation - 0.0%

| Class | Coverage | Lines Covered/Total | Branches Covered/Total |
|-------|----------|---------------------|------------------------|
| ❌ LibraryBaseRoute.kt                      |   0.0% | 0/2 | N/A |
| ❌ LibraryRoute.kt                          |   0.0% | 0/2 | N/A |
| ❌ LibraryNavigationKt.kt                   |   0.0% | 0/28 | 0/24 |

#### 📦 io.jacob.episodive.feature.library - 11.7%

| Class | Coverage | Lines Covered/Total | Branches Covered/Total |
|-------|----------|---------------------|------------------------|
| ❌ LibraryScreenKt.kt                       |   0.0% | 0/491 | 0/626 |
| ⚠️ LibraryViewModel.kt                      |  81.5% | 66/81 | N/A |
| ✅ LibrarySection.kt                        | 100.0% | 1/1 | N/A |

### feature:onboarding - 20.8% Coverage

#### 📦 io.jacob.episodive.feature.onboarding - 16.9%

| Class | Coverage | Lines Covered/Total | Branches Covered/Total |
|-------|----------|---------------------|------------------------|
| ❌ OnboardingScreenKt.kt                    |   0.0% | 0/246 | 0/198 |
| ✅ OnboardingViewModel.kt                   |  92.2% | 47/51 | N/A |
| ✅ OnboardingPage.kt                        | 100.0% | 4/4 | N/A |

### feature:player - 16.9% Coverage

#### 📦 io.jacob.episodive.feature.player - 11.4%

| Class | Coverage | Lines Covered/Total | Branches Covered/Total |
|-------|----------|---------------------|------------------------|
| ❌ PlayerScreenKt.kt                        |   0.0% | 0/551 | 0/552 |
| ❌ PlayerBarKt.kt                           |   0.0% | 0/119 | 0/88 |
| ✅ PlayerViewModel.kt                       | 100.0% | 86/86 | N/A |

### feature:podcast - 24.9% Coverage

#### 📦 io.jacob.episodive.feature.podcast.navigation - 0.0%

| Class | Coverage | Lines Covered/Total | Branches Covered/Total |
|-------|----------|---------------------|------------------------|
| ❌ PodcastRoute.kt                          |   0.0% | 0/2 | 0/2 |
| ❌ PodcastNavigationKt.kt                   |   0.0% | 0/14 | N/A |

#### 📦 io.jacob.episodive.feature.podcast - 19.8%

| Class | Coverage | Lines Covered/Total | Branches Covered/Total |
|-------|----------|---------------------|------------------------|
| ❌ PodcastScreenKt.kt                       |   0.0% | 0/134 | 0/110 |
| ✅ PodcastViewModel.kt                      | 100.0% | 33/33 | N/A |

### feature:search - 25.4% Coverage

#### 📦 io.jacob.episodive.feature.search.navigation - 0.0%

| Class | Coverage | Lines Covered/Total | Branches Covered/Total |
|-------|----------|---------------------|------------------------|
| ❌ SearchBaseRoute.kt                       |   0.0% | 0/2 | N/A |
| ❌ SearchRoute.kt                           |   0.0% | 0/2 | N/A |
| ❌ SearchNavigationKt.kt                    |   0.0% | 0/28 | 0/24 |

#### 📦 io.jacob.episodive.feature.search - 19.1%

| Class | Coverage | Lines Covered/Total | Branches Covered/Total |
|-------|----------|---------------------|------------------------|
| ❌ SearchScreenKt.kt                        |   0.0% | 0/228 | 0/364 |
| ✅ SearchViewModel.kt                       |  98.2% | 54/55 | N/A |

---

## Recommendations

### 🔴 Priority: Zero Coverage Classes

These classes have no test coverage and should be prioritized:

- **core:data/SoundbiteEpisodePagingSource.kt** (84 lines)
- **core:data/RecommendedPodcastPagingSource.kt** (70 lines)
- **feature:home/HomeScreenKt.kt** (151 lines)
- **feature:home/HomeNavigationKt.kt** (31 lines)
- **feature:home/HomeBaseRoute.kt** (2 lines)
- **feature:home/HomeRoute.kt** (2 lines)
- **feature:search/SearchBaseRoute.kt** (2 lines)
- **feature:search/SearchRoute.kt** (2 lines)
- **feature:search/SearchNavigationKt.kt** (28 lines)
- **feature:search/SearchScreenKt.kt** (228 lines)

### 🟡 Medium Priority: Low Coverage Classes (< 50%)

- **core:data/PodcastRepositoryImpl.kt** - 42.4% (49 lines missed)
- **core:data/ConnectivityManagerNetworkMonitor.kt** - 45.5% (6 lines missed)

