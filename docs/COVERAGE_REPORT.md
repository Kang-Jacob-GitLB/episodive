# Code Coverage Report

Generated: 2026-03-31 14:05:54

---

## Overall Summary

| Module | Line Coverage | Branch Coverage | Total Lines | Covered | Missed |
|--------|--------------|----------------|-------------|---------|--------|
| core:data       |  87.2% |  50.9% |    844 |    736 |    108 |
| core:database   |  77.0% |  48.2% |   3320 |   2558 |    762 |
| core:datastore  |  72.6% |  25.0% |    106 |     77 |     29 |
| core:domain     |  87.7% |  56.7% |    334 |    293 |     41 |
| core:network    |  87.2% |  58.1% |    477 |    416 |     61 |
| feature:channel |  98.0% |  53.3% |     51 |     50 |      1 |
| feature:clip    | 100.0% |  81.2% |     65 |     65 |      0 |
| feature:home    |  95.7% |  66.7% |     92 |     88 |      4 |
| feature:library |  73.2% |  26.9% |    198 |    145 |     53 |
| feature:onboarding |  95.5% |  73.1% |     89 |     85 |      4 |
| feature:player  |  91.7% |  69.4% |    228 |    209 |     19 |
| feature:podcast |  92.1% |  68.4% |     63 |     58 |      5 |
| feature:search  |  93.7% |  68.6% |    126 |    118 |      8 |
|--------|--------------|----------------|-------------|---------|--------|
| **TOTAL** | ** 81.7%** | ** 50.0%** | **  5993** | **  4898** | **  1095** |

---

## Classes with Low Coverage (< 80%)

| Module | Package | Class | Coverage | Covered/Total | Missed |
|--------|---------|-------|----------|---------------|--------|
| core:data  | io.jacob.episodive.core.data.u | ConnectivityManagerNetworkMoni |  72.7% | 8/11 | 3 |
| core:datastore | io.jacob.episodive.core.datast | UserPreferencesStore.kt        |  73.0% | 27/37 | 10 |
| feature:library | io.jacob.episodive.feature.lib | LibraryViewModel.kt            |  78.3% | 72/92 | 20 |

---

## Detailed Module Reports

### core:data - 87.2% Coverage

#### 📦 io.jacob.episodive.core.data.repository - 89.0%

| Class | Coverage | Lines Covered/Total | Branches Covered/Total |
|-------|----------|---------------------|------------------------|
| ⚠️ PodcastRepositoryImpl.kt                 |  84.1% | 69/82 | N/A |
| ⚠️ EpisodeRepositoryImpl.kt                 |  86.3% | 113/131 | N/A |
| ⚠️ PlayerRepositoryImpl.kt                  |  89.6% | 60/67 | N/A |
| ✅ UserRepositoryImpl.kt                    |  92.6% | 25/27 | 2/2 |
| ✅ ChannelRepositoryImpl.kt                 | 100.0% | 4/4 | N/A |
| ✅ RecentSearchRepositoryImpl.kt            | 100.0% | 51/51 | 29/37 |

#### 📦 io.jacob.episodive.core.data.util - 89.3%

| Class | Coverage | Lines Covered/Total | Branches Covered/Total |
|-------|----------|---------------------|------------------------|
| ❌ ConnectivityManagerNetworkMonitor.kt     |  72.7% | 8/11 | 1/4 |
| ✅ ImageCacheInterceptor.kt                 | 100.0% | 17/17 | 4/4 |

#### 📦 io.jacob.episodive.core.data.util.paging - 92.4%

| Class | Coverage | Lines Covered/Total | Branches Covered/Total |
|-------|----------|---------------------|------------------------|
| ✅ RecommendedPodcastPagingSource.kt        |  91.4% | 64/70 | 20/34 |
| ✅ SoundbiteEpisodePagingSource.kt          |  92.9% | 78/84 | 26/44 |
| ✅ PagingDefaults.kt                        | 100.0% | 4/4 | N/A |

#### 📦 io.jacob.episodive.core.data.util.updater - 95.2%

| Class | Coverage | Lines Covered/Total | Branches Covered/Total |
|-------|----------|---------------------|------------------------|
| ✅ PodcastRemoteUpdater.kt                  |  94.1% | 48/51 | 22/32 |
| ✅ EpisodeRemoteUpdater.kt                  |  94.4% | 34/36 | 13/16 |
| ✅ RemoteUpdater.kt                         | 100.0% | 18/18 | 1/2 |

### core:database - 77.0% Coverage

#### 📦 io.jacob.episodive.core.database.dao - 96.3%

| Class | Coverage | Lines Covered/Total | Branches Covered/Total |
|-------|----------|---------------------|------------------------|
| ✅ EpisodeDao.kt                            |  96.2% | 75/78 | 11/14 |
| ✅ PodcastDao.kt                            |  96.2% | 50/52 | 8/10 |
| ✅ FeedDao.kt                               | 100.0% | 3/3 | N/A |
| ✅ SoundbiteDao.kt                          | 100.0% | 3/3 | N/A |

#### 📦 io.jacob.episodive.core.database.mapper - 97.3%

| Class | Coverage | Lines Covered/Total | Branches Covered/Total |
|-------|----------|---------------------|------------------------|
| ✅ DatabaseMapperKt.kt                      |  97.3% | 216/222 | 3/6 |

#### 📦 io.jacob.episodive.core.database.util - 100.0%

| Class | Coverage | Lines Covered/Total | Branches Covered/Total |
|-------|----------|---------------------|------------------------|
| ✅ FtsExtKt.kt                              | 100.0% | 3/3 | 6/6 |
| ✅ DurationConverter.kt                     | 100.0% | 3/3 | 3/4 |
| ✅ RecentSearchTypeConverter.kt             | 100.0% | 3/3 | 2/4 |
| ✅ InstantConverter.kt                      | 100.0% | 3/3 | 4/4 |
| ✅ MediumConverter.kt                       | 100.0% | 3/3 | 2/4 |
| ✅ EpisodeTypeConverter.kt                  | 100.0% | 3/3 | 2/4 |
| ✅ DownloadStatusConverter.kt               | 100.0% | 3/3 | 4/4 |
| ✅ CategoryConverter.kt                     | 100.0% | 3/3 | N/A |

#### 📦 io.jacob.episodive.core.database.datasource - 100.0%

| Class | Coverage | Lines Covered/Total | Branches Covered/Total |
|-------|----------|---------------------|------------------------|
| ✅ PodcastLocalDataSourceImpl.kt            | 100.0% | 31/31 | 5/8 |
| ✅ EpisodeLocalDataSourceImpl.kt            | 100.0% | 56/56 | 9/16 |
| ✅ RecentSearchLocalDataSourceImpl.kt       | 100.0% | 9/9 | N/A |
| ✅ FeedLocalDataSourceImpl.kt               | 100.0% | 14/14 | N/A |
| ✅ SoundbiteLocalDataSourceImpl.kt          | 100.0% | 12/12 | N/A |

#### 📦 io.jacob.episodive.core.database - 100.0%

| Class | Coverage | Lines Covered/Total | Branches Covered/Total |
|-------|----------|---------------------|------------------------|
| ✅ EpisodiveDatabase.kt                     | 100.0% | 1/1 | N/A |

### core:datastore - 72.6% Coverage

#### 📦 io.jacob.episodive.core.datastore.store - 73.0%

| Class | Coverage | Lines Covered/Total | Branches Covered/Total |
|-------|----------|---------------------|------------------------|
| ❌ UserPreferencesStore.kt                  |  73.0% | 27/37 | 6/10 |

#### 📦 io.jacob.episodive.core.datastore.datasource - 100.0%

| Class | Coverage | Lines Covered/Total | Branches Covered/Total |
|-------|----------|---------------------|------------------------|
| ✅ UserPreferencesDataSourceImpl.kt         | 100.0% | 19/19 | N/A |

#### 📦 io.jacob.episodive.core.datastore.mapper - 100.0%

| Class | Coverage | Lines Covered/Total | Branches Covered/Total |
|-------|----------|---------------------|------------------------|
| ✅ DataStoreMapperKt.kt                     | 100.0% | 12/12 | N/A |

### core:domain - 87.7% Coverage

#### 📦 io.jacob.episodive.core.domain.usecase.channel - 100.0%

| Class | Coverage | Lines Covered/Total | Branches Covered/Total |
|-------|----------|---------------------|------------------------|
| ✅ GetChannelByIdUseCase.kt                 | 100.0% | 3/3 | N/A |
| ✅ GetChannelsUseCase.kt                    | 100.0% | 3/3 | N/A |

#### 📦 io.jacob.episodive.core.domain.usecase.episode - 100.0%

| Class | Coverage | Lines Covered/Total | Branches Covered/Total |
|-------|----------|---------------------|------------------------|
| ✅ GetEpisodeByIdUseCase.kt                 | 100.0% | 3/3 | N/A |
| ✅ GetSavedEpisodesPagingUseCase.kt         | 100.0% | 3/3 | N/A |
| ✅ ToggleLikedEpisodeUseCase.kt             | 100.0% | 4/4 | N/A |
| ✅ GetEpisodesByPodcastIdPagingUseCase.kt   | 100.0% | 4/4 | N/A |
| ✅ GetLikedEpisodesPagingUseCase.kt         | 100.0% | 3/3 | N/A |
| ✅ GetAllPlayedEpisodesUseCase.kt           | 100.0% | 8/8 | N/A |
| ✅ GetLiveEpisodesUseCase.kt                | 100.0% | 3/3 | N/A |
| ✅ GetClipEpisodesPagingUseCase.kt          | 100.0% | 3/3 | N/A |
| ✅ GetLikedEpisodesUseCase.kt               | 100.0% | 4/4 | N/A |
| ✅ GetAllPlayedEpisodesPagingUseCase.kt     | 100.0% | 7/7 | N/A |
| ✅ SaveEpisodeUseCase.kt                    | 100.0% | 11/11 | 6/6 |
| ✅ UpdatePlayedEpisodeUseCase.kt            | 100.0% | 11/11 | 2/4 |
| ✅ GetMyRandomEpisodesUseCase.kt            | 100.0% | 4/4 | N/A |
| ✅ GetPlayingEpisodesUseCase.kt             | 100.0% | 8/8 | N/A |
| ✅ GetSavedEpisodesUseCase.kt               | 100.0% | 4/4 | N/A |
| ✅ GetRecentEpisodesUseCase.kt              | 100.0% | 3/3 | N/A |
| ✅ GetChaptersUseCase.kt                    | 100.0% | 3/3 | N/A |

#### 📦 io.jacob.episodive.core.domain.usecase.search - 100.0%

| Class | Coverage | Lines Covered/Total | Branches Covered/Total |
|-------|----------|---------------------|------------------------|
| ✅ UpsertRecentSearchUseCase.kt             | 100.0% | 8/8 | N/A |
| ✅ SearchUseCase.kt                         | 100.0% | 4/4 | N/A |
| ✅ GetRecentSearchesUseCase.kt              | 100.0% | 3/3 | N/A |
| ✅ DeleteRecentSearchUseCase.kt             | 100.0% | 4/4 | N/A |
| ✅ ClearRecentSearchesUseCase.kt            | 100.0% | 4/4 | N/A |

#### 📦 io.jacob.episodive.core.domain.usecase - 100.0%

| Class | Coverage | Lines Covered/Total | Branches Covered/Total |
|-------|----------|---------------------|------------------------|
| ✅ FindInLibraryUseCase.kt                  | 100.0% | 8/8 | N/A |

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
| ✅ SaveLastPlayStateUseCase.kt              | 100.0% | 4/4 | N/A |
| ✅ GetNowPlayingUseCase.kt                  | 100.0% | 4/4 | N/A |
| ✅ PlayEpisodeUseCase.kt                    | 100.0% | 19/19 | 10/10 |
| ✅ ResumeEpisodeUseCase.kt                  | 100.0% | 5/5 | N/A |
| ✅ GetPlaylistUseCase.kt                    | 100.0% | 4/4 | N/A |
| ✅ RestoreLastPlayStateUseCase.kt           | 100.0% | 14/14 | 8/8 |

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

### feature:channel - 98.0% Coverage

#### 📦 io.jacob.episodive.feature.channel - 100.0%

| Class | Coverage | Lines Covered/Total | Branches Covered/Total |
|-------|----------|---------------------|------------------------|
| ✅ ChannelViewModel.kt                      | 100.0% | 28/28 | N/A |

### feature:clip - 100.0% Coverage

#### 📦 io.jacob.episodive.feature.clip - 100.0%

| Class | Coverage | Lines Covered/Total | Branches Covered/Total |
|-------|----------|---------------------|------------------------|
| ✅ ClipViewModel.kt                         | 100.0% | 36/36 | N/A |
| ✅ ClipPlayerState.kt                       | 100.0% | 5/5 | N/A |

### feature:home - 95.7% Coverage

#### 📦 io.jacob.episodive.feature.home - 95.3%

| Class | Coverage | Lines Covered/Total | Branches Covered/Total |
|-------|----------|---------------------|------------------------|
| ✅ HomeViewModel.kt                         |  95.3% | 41/43 | N/A |

### feature:library - 73.2% Coverage

#### 📦 io.jacob.episodive.feature.library - 78.5%

| Class | Coverage | Lines Covered/Total | Branches Covered/Total |
|-------|----------|---------------------|------------------------|
| ❌ LibraryViewModel.kt                      |  78.3% | 72/92 | N/A |
| ✅ LibrarySection.kt                        | 100.0% | 1/1 | N/A |

### feature:onboarding - 95.5% Coverage

#### 📦 io.jacob.episodive.feature.onboarding - 92.7%

| Class | Coverage | Lines Covered/Total | Branches Covered/Total |
|-------|----------|---------------------|------------------------|
| ✅ OnboardingViewModel.kt                   |  92.2% | 47/51 | N/A |
| ✅ OnboardingPage.kt                        | 100.0% | 4/4 | N/A |

### feature:player - 91.7% Coverage

#### 📦 io.jacob.episodive.feature.player - 96.1%

| Class | Coverage | Lines Covered/Total | Branches Covered/Total |
|-------|----------|---------------------|------------------------|
| ✅ PlayerViewModel.kt                       |  95.8% | 92/96 | N/A |
| ✅ LastPlaySnapshot.kt                      | 100.0% | 6/6 | N/A |

### feature:podcast - 92.1% Coverage

#### 📦 io.jacob.episodive.feature.podcast - 94.4%

| Class | Coverage | Lines Covered/Total | Branches Covered/Total |
|-------|----------|---------------------|------------------------|
| ✅ PodcastViewModel.kt                      |  94.4% | 34/36 | N/A |

### feature:search - 93.7% Coverage

#### 📦 io.jacob.episodive.feature.search - 98.3%

| Class | Coverage | Lines Covered/Total | Branches Covered/Total |
|-------|----------|---------------------|------------------------|
| ✅ SearchViewModel.kt                       |  98.3% | 57/58 | N/A |

---

## Recommendations

