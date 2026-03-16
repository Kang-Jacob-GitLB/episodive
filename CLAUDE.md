# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Episodive is an Android podcast application built with Kotlin and Jetpack Compose that uses the Podcast Index API. The app follows Clean Architecture with MVI (Model-View-Intent) pattern and implements a multi-module structure (20 modules total) using Gradle convention plugins. The architecture emphasizes offline-first design with Room database as the single source of truth.

### Technology Stack

- **Build**: Gradle 9.2.1, AGP 8.13.1, Kotlin 2.2.21
- **Target**: Min SDK 28, Target/Compile SDK 36, Java 11
- **UI**: Jetpack Compose (BOM 2025.12.00), Material3 1.5.0-alpha10
- **DI**: Hilt 2.57.2
- **Database**: Room 2.8.4 with Paging 3.3.6, Auto-migrations
- **Network**: Retrofit 3.0.0, OkHttp 5.3.2, Gson
- **Async**: Kotlin Coroutines 1.10.2, Lifecycle 2.10.0
- **Media**: Media3 1.8.0 (ExoPlayer), MediaNotificationService
- **Image**: Coil 2.7.0, Palette API
- **Testing**: JUnit 4.13.2, MockK 1.14.6, Turbine 1.2.1, Robolectric 4.16

## Build Commands

### Core Commands
- `./gradlew build` - Build all modules
- `./gradlew test` - Run unit tests for all modules
- `./gradlew check` - Run all verification tasks (tests, lint, etc.)
- `./gradlew testDebugUnitTest` - Run debug unit tests only
- `./gradlew connectedAndroidTest` - Run instrumentation tests on connected devices

### Module-specific Testing
- `./gradlew :core:database:test` - Test database module only
- `./gradlew :core:network:test` - Test network module only
- `./gradlew :core:data:test` - Test data module only
- `./gradlew :core:domain:test` - Test domain module only
- `./gradlew :feature:home:test` - Test home feature module
- `./gradlew :feature:search:test` - Test search feature module

### Code Quality
- `./gradlew lint` - Run lint analysis
- `./gradlew lintFix` - Auto-fix lint issues where possible

### Coverage
- `./gradlew createDebugCoverageReport` - Generate test coverage reports (uses Jacoco)
- `./gradlew generateCoverageReport` - Custom Python script for comprehensive coverage report

## Architecture

### Module Structure

The project follows Clean Architecture with 20 modules organized into Core, Feature, and App layers:

#### **Core Modules (11 modules):**

1. **`:core:model`** (JVM library)
   - Pure Kotlin domain models (Podcast, Episode, Category, UserData, etc.)
   - Enums with value properties: Medium, EpisodeType, Category, GroupKey
   - Dependencies: kotlinx-datetime only (no Android dependencies)

2. **`:core:domain`** (Android library)
   - Repository interfaces: PodcastRepository, EpisodeRepository, UserDataRepository, etc.
   - 43 Use Cases organized by feature:
     - Channel: GetChannelUseCase, GetChannelPodcastsUseCase
     - Episode: GetEpisodeUseCase, GetEpisodesUseCase, GetLiveEpisodesUseCase
     - Podcast: GetPodcastUseCase, GetPodcastsUseCase, GetTrendingPodcastsUseCase
     - Player: GetNowPlayingUseCase, PlayPodcastUseCase, PausePlaybackUseCase
     - Search: SearchPodcastsUseCase, GetRecentSearchesUseCase
     - User: FollowPodcastUseCase, LikeEpisodeUseCase, MarkAsPlayedUseCase
   - No implementations, only contracts and interfaces
   - Dependencies: core:common, core:model, Paging, Media3

3. **`:core:data`** (Android library)
   - Repository implementations coordinating network, database, and datastore
   - Custom patterns:
     - **RemoteUpdater**: Abstract class for managing cache refresh logic
     - **CacheableQuery**: Interface for cache key and TTL management
     - Custom **PagingSource** implementations for infinite scroll
   - Offline-first architecture with Room as single source of truth
   - Dependencies: All other core modules, Hilt, Paging, Coil, Palette

4. **`:core:network`** (Android library)
   - 5 Retrofit API interfaces: CategoryApi, EpisodeApi, PodcastApi, SearchApi, SoundbiteApi
   - **EpisodiveInterceptor**: Custom interceptor for Podcast Index API authentication (SHA-1)
   - Remote data sources with interface and implementation pattern
   - Response wrappers: ResponseListWrapper for consistent API handling
   - Dependencies: core:common, core:model, Retrofit, OkHttp, Gson

5. **`:core:database`** (Android library)
   - **Room Database** version 8 with auto-migrations
   - **12 Entities**:
     - Main: PodcastEntity, EpisodeEntity, FeedEntity, SoundbiteEntity
     - User Interaction: FollowedPodcastEntity, LikedEpisodeEntity, PlayedEpisodeEntity
     - Grouping: PodcastGroupEntity, EpisodeGroupEntity (for cache management)
     - FTS: PodcastFtsEntity, EpisodeFtsEntity (Full-Text Search)
     - Search: RecentSearchEntity
   - **2 Database Views**:
     - PodcastWithExtrasView: Joins podcasts with followed status
     - EpisodeWithExtrasView: Joins episodes with liked/played/clip data
   - **5 DAOs**: PodcastDao, EpisodeDao, FeedDao, SoundbiteDao, RecentSearchDao
   - Custom **TypeConverters** for enums, Instant, Duration, collections
   - All entities have caching fields: cachedAt (Instant), grouping keys
   - Dependencies: core:common, core:model, Room with KSP, Gson

6. **`:core:datastore`** (Android library)
   - DataStore Preferences for user settings
   - UserPreferences model: isFirstLaunch, language, categories, playbackSpeed
   - Dependencies: core:common, core:model, DataStore Preferences

7. **`:core:player`** (Android library)
   - ExoPlayer wrapper with Flow-based reactive APIs
   - **Dual player support** using @Player qualifier:
     - @Player(EpisodivePlayers.Main) - Main episode playback
     - @Player(EpisodivePlayers.Clip) - Soundbite/clip playback
   - PlayerDataSource interface exposing:
     - nowPlaying, progress, isPlaying, playlist, playbackSpeed, etc.
   - Dependencies: core:common, core:model, Media3 ExoPlayer

8. **`:core:common`** (JVM library)
   - Shared utilities and Hilt qualifiers
   - **EpisodiveDispatchers** enum (Default, IO) with @Dispatcher qualifier
   - **EpisodivePlayers** enum (Main, Clip) with @Player qualifier
   - Flow extensions and utility functions
   - Pure Kotlin, no Android dependencies

9. **`:core:designsystem`** (Android library with Compose)
   - 60+ reusable Compose components
   - Theme system: Color, Type, Dimension, Gradient, Background
   - Custom Tabler icons
   - Seeker component for audio scrubbing
   - Dependencies: core:common, core:domain, core:model, Compose, Coil, Palette, Paging

10. **`:core:ui`** (Android library with Compose)
    - Higher-level domain-specific UI components
    - Category, Channel, Episode, Podcast cards and lists
    - Dependencies: core:designsystem, core:model, Compose, Coil, Palette, Paging

11. **`:core:testing`** (Android library)
    - Test data factories: PodcastTestData, EpisodeTestData, FeedTestData, ChannelTestData
    - Test utilities: MainDispatcherRule, RoomDatabaseRule, DisabledOnWindowsRule
    - Paging test extensions
    - Dependencies: core:model, JUnit, Coroutines Test, MockK, Paging Testing

#### **Feature Modules (8 modules):**

All feature modules use the `episodive.android.feature` convention plugin which automatically includes:
- Compose, Hilt, Jacoco, Test dependencies
- Base dependencies: core:common, core:domain, core:designsystem, core:model, core:ui, core:testing
- Standard libraries: Compose, Paging, Hilt Navigation, Lifecycle, Navigation

1. **`:feature:onboarding`** - Onboarding flow with category selection
2. **`:feature:home`** - Main feed with recent, trending, random podcast content
3. **`:feature:search`** - Search and browse with FTS (Full-Text Search) support
4. **`:feature:library`** - User's followed podcasts, liked episodes, played episodes
5. **`:feature:podcast`** - Podcast details and episode list
6. **`:feature:player`** - Audio player UI with controls
7. **`:feature:clip`** - Soundbites and clips browsing
8. **`:feature:channel`** - Channel/category browsing

All features follow **MVI pattern**:
- **State**: Sealed interface (Loading | Success | Error)
- **Action**: Sealed interface for user intents (ClickPodcast, QueryChanged, etc.)
- **Effect**: One-time side effects (NavigateToPodcast, ShowError)
- **ViewModel**: Handles actions, emits state and effects

#### **App Module:**

- **`:app`** - Main Android application
  - Nested navigation with bottom bar (Home, Search, Library, Clip)
  - **MediaNotificationService** for background playback (Media3 Session)
  - Hilt application setup with custom Timber logging
  - Coil ImageLoader configuration
  - Entry point for the entire application

### Key Architecture Patterns

#### **Clean Architecture Layers:**
```
UI (Feature) → Domain (Use Cases) → Data (Repositories) → Data Sources (Network/Database/DataStore)
```

#### **MVI (Model-View-Intent) Pattern:**
All feature modules follow MVI:
- **State**: Sealed interface representing UI state (Loading | Success | Error)
- **Action**: Sealed interface for user intents (ClickPodcast, QueryChanged, ToggleLike, etc.)
- **Effect**: One-time side effects (NavigateToPodcast, ShowError, ShowToast)
- **ViewModel**: Processes actions, emits state via StateFlow, emits effects via SharedFlow

Example from SearchViewModel:
```kotlin
sealed interface SearchState {
    data object Loading : SearchState
    data class Success(val podcasts: PagingData<Podcast>) : SearchState
    data class Error(val message: String) : SearchState
}

sealed interface SearchAction {
    data class QueryChanged(val query: String) : SearchAction
    data object ClickSearch : SearchAction
    data class ClickPodcast(val podcastId: Long) : SearchAction
}

sealed interface SearchEffect {
    data class NavigateToPodcast(val podcastId: Long) : SearchEffect
}
```

#### **Offline-First Architecture:**
- **Single Source of Truth**: Room database is authoritative
- **Reactive Streams**: Kotlin Flow everywhere for reactive data
- **Cache Strategy**: RemoteUpdater pattern manages cache refresh
  - Checks cache expiration via CacheableQuery (key + TTL)
  - Fetches from remote API if expired
  - Converts API models to entities and stores in database
  - Emits database data as Flow or PagingData
- **Database Views**: Join tables with user interaction data for efficient queries

#### **Data Caching Pattern (RemoteUpdater):**
Abstract class for managing cache refresh logic:
1. Check if cache is expired using CacheableQuery
2. If expired, fetch from remote API
3. Convert API response models to database entities
4. Store entities in Room database with timestamp
5. Return Flow from database (single source of truth)

**CacheableQuery Types:**
- **PodcastQuery**: FeedId, Medium, Trending, Recommended, Random, Recent
- **EpisodeQuery**: FeedId, Live, Random, Recent, RecentNew

#### **Dual Player System:**
Two separate ExoPlayer instances managed via Hilt @Player qualifier:
- **@Player(EpisodivePlayers.Main)**: Main episode playback (full episodes)
- **@Player(EpisodivePlayers.Clip)**: Soundbite/clip playback (short clips)

This allows simultaneous management of different playback contexts without interference.

#### **Full-Text Search (FTS):**
- Room FTS4 entities: PodcastFtsEntity, EpisodeFtsEntity
- Synchronized with main entities via database triggers
- Fast search with ranking and highlighting support

#### **Paging 3 Integration:**
- Custom PagingSource implementations for infinite scroll
- PagingData flows from repository → ViewModel → UI
- Supports both network and database pagination
- Integrated with Compose LazyColumn for efficient rendering

### Build Logic (Convention Plugins)

Located in `build-logic/convention/`, these Gradle convention plugins ensure consistent configuration:

1. **episodive.android.application** - Application module setup with Android configuration
2. **episodive.android.application.compose** - Compose setup for app module
3. **episodive.android.application.jacoco** - Test coverage for app module
4. **episodive.android.library** - Standard Android library configuration
5. **episodive.android.library.compose** - Compose setup for libraries
6. **episodive.android.library.jacoco** - Test coverage for libraries
7. **episodive.android.feature** - Feature module template (combines library, compose, hilt, test, jacoco)
   - Automatically includes: core:common, core:domain, core:designsystem, core:model, core:ui, core:testing
   - Standard dependencies: Compose, Paging, Hilt Navigation, Lifecycle, Navigation
8. **episodive.android.room** - Room database configuration with KSP and schema directory
9. **episodive.android.test** - Test dependencies (JUnit, Robolectric, MockK, Turbine, Coroutines Test)
10. **episodive.hilt** - Hilt dependency injection setup with KSP
11. **episodive.jvm.library** - Pure Kotlin/JVM library (no Android dependencies)

**Usage Example:**
```kotlin
// In feature module's build.gradle.kts
plugins {
    alias(libs.plugins.episodive.android.feature)
}
// Automatically gets Compose, Hilt, Testing, and all core dependencies
```

### Important Implementation Details

#### **1. Enum Handling (CRITICAL)**

All enums use **value properties** instead of enum names:

```kotlin
enum class Medium(val value: String) {
    PODCAST("podcast"),
    MUSIC("music"),
    VIDEO("video"),
    FILM("film"),
    AUDIOBOOK("audiobook"),
    NEWSLETTER("newsletter"),
    BLOG("blog"),
    PUBLISHER("publisher"),
    COURSE("course")
}

enum class EpisodeType(val value: String) {
    FULL("full"),
    TRAILER("trailer"),
    BONUS("bonus")
}

enum class Category(val value: String) {
    BUSINESS("Business"),
    COMEDY("Comedy"),
    EDUCATION("Education"),
    // ... etc
}
```

**Conversion Pattern** (used everywhere):
```kotlin
// CORRECT: Use entries.find()
fun String.toMedium(): Medium? = Medium.entries.find { it.value == this }

// WRONG: Do NOT use valueOf()
// valueOf() expects enum name ("PODCAST") not value ("podcast")
```

**Room TypeConverters** must use this pattern:
```kotlin
@TypeConverter
fun toMedium(value: String?): Medium? = value?.toMedium()

@TypeConverter
fun fromMedium(medium: Medium?): String? = medium?.value
```

**Why**: API returns lowercase values (`"podcast"`), but enum names are uppercase (`PODCAST`). Using `valueOf()` would fail.

#### **2. Database Schema Details**

**Main Entities:**
- `PodcastEntity` - Cached podcast data with metadata
- `EpisodeEntity` - Episode data with relationships
- `FeedEntity` - Feed metadata and grouping
- `SoundbiteEntity` - Soundbite/clip data

**User Interaction Entities:**
- `FollowedPodcastEntity` - User's subscribed podcasts
- `LikedEpisodeEntity` - Liked episodes with timestamp
- `PlayedEpisodeEntity` - Playback history with progress
- `RecentSearchEntity` - Search history

**Grouping Entities (for cache management):**
- `PodcastGroupEntity` - Groups podcasts by query type
- `EpisodeGroupEntity` - Groups episodes by query type

**FTS Entities (Full-Text Search):**
- `PodcastFtsEntity` - FTS4 index for podcast search
- `EpisodeFtsEntity` - FTS4 index for episode search

**Database Views (for efficient queries):**
- `PodcastWithExtrasView` - JOIN podcasts + followed status
- `EpisodeWithExtrasView` - JOIN episodes + liked/played/clip data

**Caching Fields (all entities have these):**
- `cachedAt: Instant` - Timestamp when data was cached
- Group keys (feedId, medium, etc.) - For cache invalidation

**Auto-migrations:**
- Room database version 8 with migration history
- Schema exported to `schemas/` directory

#### **3. API Authentication (Podcast Index)**

Custom `EpisodiveInterceptor` adds authentication headers:

```kotlin
X-Auth-Date: <unix_timestamp>
X-Auth-Key: <api_key>
Authorization: <sha1(apiKey + apiSecret + timestamp)>
User-Agent: Episodive/<version>
```

SHA-1 signature calculation:
```kotlin
val dataToHash = apiKey + apiSecret + unixTimestamp
val sha1Hash = MessageDigest.getInstance("SHA-1")
    .digest(dataToHash.toByteArray())
    .joinToString("") { "%02x".format(it) }
```

#### **4. Response Wrappers**

Consistent API response handling:
```kotlin
@JsonClass(generateAdapter = true)
data class ResponseListWrapper<T>(
    @Json(name = "status") val status: String,
    @Json(name = "feeds") val feeds: List<T>? = null,
    @Json(name = "items") val items: List<T>? = null,
    @Json(name = "channels") val channels: List<T>? = null,
    @Json(name = "description") val description: String? = null,
    @Json(name = "count") val count: Int? = null
)
```

Different endpoints use different JSON field names; wrapper handles all variations.

#### **5. Player Integration (Dual Players)**

Two separate ExoPlayer instances via Hilt:

```kotlin
// Main player for full episodes
@Provides
@Player(EpisodivePlayers.Main)
fun provideMainExoPlayer(@ApplicationContext context: Context): ExoPlayer

// Clip player for soundbites
@Provides
@Player(EpisodivePlayers.Clip)
fun provideClipExoPlayer(@ApplicationContext context: Context): ExoPlayer
```

**PlayerDataSource** interface exposes:
- `nowPlaying: Flow<Episode?>` - Current playing episode
- `progress: Flow<Long>` - Playback position in milliseconds
- `isPlaying: Flow<Boolean>` - Playing state
- `playlist: Flow<List<Episode>>` - Current playlist
- `playbackSpeed: Flow<Float>` - Playback speed multiplier

**Background Playback:**
- `MediaNotificationService` extends Media3 MediaSessionService
- Handles notification controls (play, pause, next, previous)
- Supports Android Auto and Wear OS

#### **6. User Preferences (DataStore)**

Stored preferences:
```kotlin
data class UserPreferences(
    val isFirstLaunch: Boolean = true,
    val language: String = "en",
    val categories: Set<Category> = emptySet(),
    val playbackSpeed: Float = 1.0f
)
```

DataStore Preferences API for type-safe storage.

## Development Workflow

### When Making Database Changes:

1. **Update Entity** in `:core:database`
   - Add/modify fields in entity class
   - Update `@ColumnInfo` annotations if needed
   - Increment Room database version if schema changes
2. **Update DAO** in `:core:database`
   - Add/modify queries in corresponding DAO interface
   - Use `@Query`, `@Insert`, `@Update`, `@Delete` annotations
3. **Add/Update TypeConverters** as needed
   - For enums, use `entries.find { it.value == stringValue }` pattern
   - For complex types (Instant, Duration, List), add custom converters
4. **Update Database Views** if entity is used in views
   - Regenerate PodcastWithExtrasView or EpisodeWithExtrasView
5. **Update Mappers** in `:core:data`
   - Add conversion functions between Entity ↔ Model
6. **Write Tests** in `:core:database`
   - Use RoomDatabaseRule for in-memory database
   - Use Turbine for testing Flow emissions
   - Follow existing test patterns

### When Adding New API Endpoints:

1. **Create Response Models** in `:core:network/model`
   - Add `@JsonClass(generateAdapter = true)` annotation
   - Use `@Json(name = "field_name")` for field mapping
2. **Add to API Interface** in `:core:network/api`
   - Define endpoint with Retrofit annotations (`@GET`, `@POST`, etc.)
   - Use appropriate response wrapper (ResponseListWrapper)
3. **Implement RemoteDataSource** in `:core:network/datasource`
   - Create interface in `datasource/` package
   - Implement in `datasource/impl/` package
4. **Update Repository** in `:core:data`
   - Add method to repository interface in `:core:domain`
   - Implement in `:core:data` using RemoteUpdater pattern
5. **Create Use Case** in `:core:domain`
   - Follow naming convention: `Get<Entity>UseCase`, `Update<Entity>UseCase`
   - Inject repository and invoke in `operator fun invoke()`
6. **Wire to ViewModel** in feature module
   - Inject use case into ViewModel
   - Handle in MVI action/state flow

### When Testing Database Code:

```kotlin
@RunWith(RobolectricTestRunner::class)
class PodcastDaoTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @get:Rule
    val databaseRule = RoomDatabaseRule()

    private lateinit var podcastDao: PodcastDao

    @Before
    fun setup() {
        podcastDao = databaseRule.database.podcastDao()
    }

    @Test
    fun insertAndRetrievePodcast() = runTest {
        // Given
        val podcast = PodcastTestData.podcasts[0]

        // When
        podcastDao.insert(podcast.toEntity())

        // Then
        podcastDao.getPodcast(podcast.id).test {
            assertEquals(podcast.id, awaitItem()?.id)
        }
    }
}
```

**Required Test Setup:**
- Use `RobolectricTestRunner` for Room tests
- Apply `MainDispatcherRule` for coroutines
- Use `RoomDatabaseRule` for in-memory database
- Use Turbine's `.test { }` for Flow testing
- Use test data from `:core:testing` module

### When Adding New Features:

1. **Create Feature Module**
   ```kotlin
   // In settings.gradle.kts
   include(":feature:myfeature")

   // In feature/myfeature/build.gradle.kts
   plugins {
       alias(libs.plugins.episodive.android.feature)
   }
   ```

2. **Follow MVI Pattern**
   - Create `MyFeatureState` sealed interface
   - Create `MyFeatureAction` sealed interface
   - Create `MyFeatureEffect` sealed interface (optional)
   - Create `MyFeatureViewModel` extending ViewModel

3. **Create Composable Screen**
   ```kotlin
   @Composable
   fun MyFeatureScreen(
       viewModel: MyFeatureViewModel = hiltViewModel(),
       onNavigateBack: () -> Unit
   ) {
       val state by viewModel.state.collectAsStateWithLifecycle()

       LaunchedEffect(Unit) {
           viewModel.effect.collect { effect ->
               when (effect) {
                   is MyFeatureEffect.NavigateBack -> onNavigateBack()
               }
           }
       }

       MyFeatureContent(
           state = state,
           onAction = viewModel::onAction
       )
   }
   ```

4. **Wire Navigation** in `:app` module
   - Add route constant
   - Add to NavHost in MainActivity
   - Add navigation calls from other screens

### Repository Pattern Guidelines:

1. **Define Interface** in `:core:domain/repository`
   ```kotlin
   interface PodcastRepository {
       fun getPodcast(id: Long): Flow<Podcast?>
       fun getTrendingPodcasts(): Flow<PagingData<Podcast>>
   }
   ```

2. **Implement in** `:core:data/repository`
   ```kotlin
   class PodcastRepositoryImpl @Inject constructor(
       private val podcastDao: PodcastDao,
       private val remoteDataSource: PodcastRemoteDataSource,
       @Dispatcher(EpisodiveDispatchers.IO) private val ioDispatcher: CoroutineDispatcher
   ) : PodcastRepository {
       // Implementation using RemoteUpdater pattern
   }
   ```

3. **Provide via Hilt** in `:core:data/di`
   ```kotlin
   @Module
   @InstallIn(SingletonComponent::class)
   interface DataModule {
       @Binds
       fun bindsPodcastRepository(
           podcastRepository: PodcastRepositoryImpl
       ): PodcastRepository
   }
   ```

## Code Editing Rules

**CRITICAL: Import Order Rule**

- **ALWAYS modify/add actual code FIRST, then add necessary imports LAST**
- NEVER add imports before writing the actual code that uses them
- This prevents linter conflicts and ensures imports are only added when actually needed
- Example order: 1) Edit function code 2) Add new composables 3) Then add required imports

## Test Data

The `:core:testing` module provides comprehensive test data factories:

### Available Test Data:

1. **PodcastTestData**
   - `podcasts: List<PodcastEntity>` - 10 sample podcast entities
   - Various categories, mediums, and metadata
   - Realistic podcast data for testing

2. **EpisodeTestData**
   - `episodes: List<EpisodeEntity>` - 10 sample episode entities
   - Different episode types (full, trailer, bonus)
   - Various durations and publication dates

3. **FeedTestData**
   - `trending: List<FeedEntity>` - Trending feed data
   - `recent: List<FeedEntity>` - Recent episodes
   - `recentNew: List<FeedEntity>` - New podcast feeds
   - `soundbites: List<FeedEntity>` - Soundbite data

4. **ChannelTestData**
   - Sample channel/category data
   - For testing channel browsing features

### Test Utilities:

1. **MainDispatcherRule** - JUnit rule for coroutine testing
   - Sets main dispatcher to test dispatcher
   - Automatically cleans up after tests

2. **RoomDatabaseRule** - JUnit rule for database testing
   - Creates in-memory Room database
   - Provides clean database for each test
   - Auto-closes database after tests

3. **DisabledOnWindowsRule** - Conditional test execution
   - Skip tests on Windows platform
   - Useful for platform-specific issues

4. **Paging Test Extensions**
   - Extensions for testing PagingData
   - Helper functions for verifying paged content

### Usage Guidelines:

**DO:**
- Always use test data from `:core:testing` module
- Use consistent test data across all modules
- Leverage test utilities (MainDispatcherRule, RoomDatabaseRule)
- Use Turbine for testing Flow emissions

**DON'T:**
- Create inline test objects (use factories instead)
- Create duplicate test data in individual modules
- Skip test utilities (they ensure consistent test environment)

### Example Test Setup:

```kotlin
@RunWith(RobolectricTestRunner::class)
class MyDaoTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @get:Rule
    val databaseRule = RoomDatabaseRule()

    private lateinit var myDao: MyDao

    @Before
    fun setup() {
        myDao = databaseRule.database.myDao()
    }

    @Test
    fun testExample() = runTest {
        // Use test data from core:testing
        val podcast = PodcastTestData.podcasts[0]

        myDao.insert(podcast)

        myDao.getAll().test {
            val items = awaitItem()
            assertEquals(1, items.size)
            assertEquals(podcast.id, items[0].id)
        }
    }
}
```

## Common Patterns and Best Practices

### 1. Dependency Injection with Hilt

**Module Organization:**
- Interface bindings in `:core:data/di`
- Providers for third-party libraries
- Scoping: Use `@Singleton` for app-wide instances

**Qualifiers for Disambiguation:**
```kotlin
// For dispatchers
@Dispatcher(EpisodiveDispatchers.IO) val ioDispatcher: CoroutineDispatcher

// For players
@Player(EpisodivePlayers.Main) val mainPlayer: ExoPlayer
```

### 2. Flow Usage

**Prefer Flow for Reactive Data:**
- DAO methods return `Flow<T>` for single items
- Return `Flow<List<T>>` for collections
- Use `Flow<PagingData<T>>` for paginated data

**StateFlow vs SharedFlow:**
- `StateFlow` for UI state (always has current value)
- `SharedFlow` for one-time effects (navigation, toasts)

### 3. Coroutine Scoping

**ViewModel:**
```kotlin
viewModelScope.launch {
    // Cancelled when ViewModel is cleared
}
```

**Repository/Use Case:**
- Don't create scopes; use suspend functions
- Let caller (ViewModel) manage lifecycle

**Background Work:**
```kotlin
@Dispatcher(EpisodiveDispatchers.IO) val ioDispatcher: CoroutineDispatcher

withContext(ioDispatcher) {
    // IO-bound work
}
```

### 4. Navigation Pattern

**Nested Navigation:**
- Bottom bar sections (Home, Search, Library, Clip)
- Each section has its own NavHost
- Pass lambda callbacks for navigation events

**Type-safe Routes:**
```kotlin
object Routes {
    const val HOME = "home"
    const val PODCAST = "podcast/{podcastId}"

    fun podcastRoute(podcastId: Long) = "podcast/$podcastId"
}
```

### 5. Error Handling

**Repository Layer:**
- Catch exceptions from network/database
- Convert to domain-specific errors
- Emit via Result type or sealed state

**UI Layer:**
- Display errors in Error state
- Show toast/snackbar via effects
- Provide retry actions

### 6. Performance Optimization

**Lazy Loading:**
- Use Paging 3 for long lists
- Load data on-demand, not upfront

**Image Loading:**
- Coil with placeholder/error drawables
- Automatic memory caching
- Palette API for dominant colors

**Database Queries:**
- Use database views for complex joins
- Index frequently queried columns
- Limit query results when possible

**Compose Performance:**
- Remember expensive calculations
- Use `derivedStateOf` for computed state
- Avoid recomposition with `key()` where appropriate

### 7. Code Style

**Naming Conventions:**
- ViewModels: `<Feature>ViewModel`
- Use Cases: `Get<Entity>UseCase`, `Update<Entity>UseCase`
- Repositories: `<Entity>Repository`
- DAOs: `<Entity>Dao`
- Entities: `<Entity>Entity`

**Package Structure:**
- Group by feature, not layer
- Keep related code together
- Avoid deep nesting

**Import Order (CRITICAL):**
- Always modify/add actual code FIRST
- Add necessary imports LAST
- This prevents linter conflicts
