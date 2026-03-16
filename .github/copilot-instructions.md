# Copilot Instructions

## 응답 언어

코드 리뷰 코멘트, 설명, 제안 등 모든 응답을 한국어로 작성하세요.

## 프로젝트 개요

Episodive는 Podcast Index API를 사용하는 Android 팟캐스트 앱입니다. Kotlin + Jetpack Compose로 구축되었으며, Clean Architecture + MVI 패턴을 따르는 멀티모듈(20개) 프로젝트입니다. 오프라인 우선 설계로 Room 데이터베이스가 단일 진실 공급원(Single Source of Truth)입니다.

## 기술 스택

- **빌드**: Gradle 9.2.1, AGP 8.13.1, Kotlin 2.2.21
- **타겟**: Min SDK 28, Target/Compile SDK 36, Java 11
- **UI**: Jetpack Compose (BOM 2025.12.00), Material3
- **DI**: Hilt 2.57.2
- **DB**: Room 2.8.4, Paging 3.3.6
- **네트워크**: Retrofit 3.0.0, OkHttp 5.3.2, Gson
- **비동기**: Kotlin Coroutines 1.10.2
- **미디어**: Media3 1.8.0 (ExoPlayer)
- **이미지**: Coil 2.7.0, Palette API
- **테스트**: JUnit 4, MockK 1.14.6, Turbine 1.2.1, Robolectric 4.16

## 아키텍처

### 계층 구조

```
UI (Feature) → Domain (Use Cases) → Data (Repositories) → Data Sources (Network/Database/DataStore)
```

### MVI 패턴

모든 Feature 모듈은 MVI 패턴을 따릅니다:

- **State**: sealed interface (Loading | Success | Error)
- **Action**: sealed interface (사용자 인텐트)
- **Effect**: sealed interface (일회성 사이드 이펙트)
- **ViewModel**: Action 처리 → StateFlow로 State 방출, SharedFlow로 Effect 방출

```kotlin
sealed interface MyFeatureState {
    data object Loading : MyFeatureState
    data class Success(val data: SomeData) : MyFeatureState
    data class Error(val message: String) : MyFeatureState
}

sealed interface MyFeatureAction {
    data class ClickItem(val id: Long) : MyFeatureAction
}

sealed interface MyFeatureEffect {
    data class NavigateToDetail(val id: Long) : MyFeatureEffect
}
```

### 모듈 구조

- **Core 모듈 (11개)**: model, domain, data, network, database, datastore, player, common, designsystem, ui, testing
- **Feature 모듈 (8개)**: onboarding, home, search, library, podcast, player, clip, channel
- **App 모듈**: 메인 앱, 네비게이션, MediaNotificationService

Feature 모듈은 `episodive.android.feature` 컨벤션 플러그인을 사용하며, Compose/Hilt/테스트 의존성이 자동 포함됩니다.

## 코딩 컨벤션

### Enum 처리 (중요)

모든 Enum은 `value` 프로퍼티를 사용합니다. API가 소문자 값을 반환하므로 `valueOf()` 대신 `entries.find`를 사용하세요.

```kotlin
// 올바른 방법
fun String.toMedium(): Medium? = Medium.entries.find { it.value == this }

// 잘못된 방법 - 사용 금지
Medium.valueOf(this) // "podcast"로 호출하면 실패 (enum 이름은 "PODCAST")
```

Room TypeConverter도 동일한 패턴:

```kotlin
@TypeConverter
fun toMedium(value: String?): Medium? = value?.toMedium()

@TypeConverter
fun fromMedium(medium: Medium?): String? = medium?.value
```

### Import 순서 (중요)

1. 실제 코드를 먼저 수정/추가
2. 필요한 import를 나중에 추가

이 순서를 지키지 않으면 린터 충돌이 발생합니다.

### 네이밍 컨벤션

| 유형 | 패턴 | 예시 |
|------|------|------|
| ViewModel | `<Feature>ViewModel` | `SearchViewModel` |
| Use Case | `Get<Entity>UseCase`, `Update<Entity>UseCase` | `GetPodcastUseCase` |
| Repository | `<Entity>Repository` / `<Entity>RepositoryImpl` | `PodcastRepository` |
| DAO | `<Entity>Dao` | `PodcastDao` |
| Entity | `<Entity>Entity` | `PodcastEntity` |

### Flow 사용

- DAO 메서드는 `Flow<T>` 또는 `Flow<List<T>>` 반환
- 페이징 데이터는 `Flow<PagingData<T>>` 사용
- UI 상태는 `StateFlow`, 일회성 이벤트는 `SharedFlow` 사용

### Hilt 한정자

```kotlin
// 디스패처 구분
@Dispatcher(EpisodiveDispatchers.IO) val ioDispatcher: CoroutineDispatcher

// 플레이어 구분 (메인 재생 / 클립 재생)
@Player(EpisodivePlayers.Main) val mainPlayer: ExoPlayer
@Player(EpisodivePlayers.Clip) val clipPlayer: ExoPlayer
```

## 데이터 캐싱 패턴

`RemoteUpdater` 추상 클래스를 사용한 캐시 관리:

1. `CacheableQuery`로 캐시 만료 확인
2. 만료 시 원격 API에서 데이터 가져오기
3. API 응답을 Entity로 변환 후 Room에 저장
4. Room에서 Flow로 데이터 반환 (Single Source of Truth)

## 테스트 패턴

`:core:testing` 모듈의 테스트 데이터와 유틸리티를 사용하세요.

```kotlin
@RunWith(RobolectricTestRunner::class)
class MyDaoTest {
    @get:Rule val mainDispatcherRule = MainDispatcherRule()
    @get:Rule val databaseRule = RoomDatabaseRule()

    private lateinit var myDao: MyDao

    @Before
    fun setup() {
        myDao = databaseRule.database.myDao()
    }

    @Test
    fun testExample() = runTest {
        val podcast = PodcastTestData.podcasts[0]
        myDao.insert(podcast)

        myDao.getAll().test {
            val items = awaitItem()
            assertEquals(1, items.size)
        }
    }
}
```

- **테스트 데이터**: `PodcastTestData`, `EpisodeTestData`, `FeedTestData`, `ChannelTestData`
- **유틸리티**: `MainDispatcherRule` (코루틴), `RoomDatabaseRule` (인메모리 DB)
- **Flow 테스트**: Turbine의 `.test { }` 블록 사용
- 인라인 테스트 객체 대신 항상 `:core:testing`의 팩토리 사용
