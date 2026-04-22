# CLAUDE.md

이 파일은 Claude Code(claude.ai/code)가 이 저장소에서 작업할 때 참고하는 가이드입니다.

## 프로젝트 개요

Episodive는 Kotlin과 Jetpack Compose로 만든 Android 팟캐스트 앱으로, Podcast Index API를 사용합니다. Clean Architecture + MVI 패턴, Gradle 컨벤션 플러그인 기반 멀티모듈 구조(20개 모듈), Room DB 중심의 Offline-First 설계를 따릅니다.

### 기술 스택

- **Build**: Gradle 9.2.1, AGP 8.13.1, Kotlin 2.2.21, KSP 2.3.1
- **Target**: Min SDK 28, Target/Compile SDK 36, Java 11
- **UI**: Jetpack Compose (BOM 2025.12.00), Material3 1.5.0-alpha10
- **DI**: Hilt 2.57.2
- **Database**: Room 2.8.4, Paging 3.3.6, Auto-migrations (버전 8)
- **Network**: Retrofit 3.0.0, OkHttp 5.3.2, Gson 2.13.2
- **Async**: Kotlin Coroutines 1.10.2, Lifecycle 2.10.0
- **Background**: WorkManager 2.10.1, Hilt Worker
- **Media**: Media3 1.8.0 (ExoPlayer), MediaNotificationService
- **Image**: Coil 2.7.0, Palette API
- **Testing**: JUnit 4.13.2, MockK 1.14.6, Turbine 1.2.1, Robolectric 4.16

## 빌드 명령어

```bash
./gradlew build                          # 전체 빌드
./gradlew test                           # 유닛 테스트 실행
./gradlew testDebugUnitTest              # Debug 유닛 테스트만 실행
./gradlew connectedAndroidTest           # 기기 연결 테스트
./gradlew :core:database:test            # 특정 모듈 테스트
./gradlew lint                           # Lint 검사
./gradlew lintFix                        # Lint 자동 수정
./gradlew createDebugCoverageReport      # 커버리지 리포트 생성 (Jacoco)
```

## 아키텍처

### 모듈 구조 (20개)

#### Core 모듈 (11개)

| 모듈 | 역할 |
|:----|:----|
| `:core:model` | 순수 Kotlin 도메인 모델 (Podcast, Episode, Category, UserData 등) |
| `:core:domain` | Repository 인터페이스 + 40개 이상의 Use Case |
| `:core:data` | Repository 구현체, RemoteUpdater 캐싱 패턴 |
| `:core:network` | Retrofit API 인터페이스 5개, EpisodiveInterceptor (SHA-1 인증) |
| `:core:database` | Room DB v8, Entity 12개, View 2개, DAO 5개 |
| `:core:datastore` | DataStore Preferences 기반 사용자 설정 관리 |
| `:core:player` | ExoPlayer 래퍼, @Player qualifier로 듀얼 플레이어(Main/Clip) |
| `:core:common` | 공유 유틸리티, EpisodiveDispatchers, EpisodivePlayers qualifier |
| `:core:designsystem` | 재사용 Compose 컴포넌트 20개 이상, 테마 시스템 |
| `:core:ui` | 도메인 특화 상위 레벨 UI 컴포넌트 |
| `:core:testing` | 테스트 데이터 팩토리, MainDispatcherRule, RoomDatabaseRule |

#### Feature 모듈 (8개)

모든 feature 모듈은 `episodive.android.feature` 컨벤션 플러그인을 사용하며, core:common/domain/designsystem/model/ui/testing과 Compose/Paging/Hilt/Navigation이 자동으로 포함됩니다.

| 모듈 | 설명 |
|:----|:----|
| `:feature:onboarding` | 온보딩 플로우, 카테고리 선택 |
| `:feature:home` | 홈 피드: 최근/트렌딩/랜덤/라이브 콘텐츠 |
| `:feature:search` | FTS 지원 검색, 최근 검색 기록 |
| `:feature:library` | 구독 팟캐스트, 좋아요/재생 기록 에피소드 |
| `:feature:podcast` | 팟캐스트 상세 정보 및 에피소드 목록 |
| `:feature:player` | 오디오 플레이어 UI |
| `:feature:clip` | 사운드바이트 및 클립 탐색 |
| `:feature:channel` | 채널/카테고리 탐색 |

### 컨벤션 플러그인 (build-logic/convention/)

| 플러그인 | 역할 |
|:--------|:----|
| `episodive.android.application` | Application 모듈 설정 |
| `episodive.android.application.compose` | App 모듈 Compose 설정 |
| `episodive.android.library` | 표준 Android 라이브러리 |
| `episodive.android.library.compose` | 라이브러리 Compose 설정 |
| `episodive.android.feature` | Feature 템플릿 (library + compose + hilt + test + jacoco) |
| `episodive.android.room` | Room + KSP + 스키마 디렉토리 설정 |
| `episodive.android.test` | 테스트 의존성 |
| `episodive.android.application.jacoco` / `episodive.android.library.jacoco` | 커버리지 설정 |
| `episodive.hilt` | Hilt DI + KSP |
| `episodive.jvm.library` | 순수 Kotlin/JVM 라이브러리 |

## 핵심 아키텍처 패턴

### Clean Architecture 계층
```
UI (Feature) → Domain (Use Cases) → Data (Repositories) → Data Sources (Network/Database/DataStore)
```

### MVI 패턴
모든 feature 모듈에서 사용:
- **State**: `sealed interface` (Loading | Success | Error)
- **Action**: 사용자 의도를 표현하는 `sealed interface`
- **Effect**: 일회성 사이드 이펙트(네비게이션, 토스트)를 `SharedFlow`로 전달
- **ViewModel**: Action을 처리하고, State는 `StateFlow`, Effect는 `SharedFlow`로 방출

### Offline-First / RemoteUpdater 패턴
`RemoteUpdater` 추상 클래스가 캐시 갱신을 관리:
1. `CacheableQuery`(키 + TTL)로 캐시 만료 여부 확인
2. 만료 시 원격 API에서 데이터 페치
3. API 모델 → Entity 변환 후 타임스탬프와 함께 Room에 저장
4. DB에서 `Flow`로 반환 (단일 소스)

**CacheableQuery 종류:**
- `PodcastQuery`: FeedId, Medium, Trending, Recommended, Random, Recent
- `EpisodeQuery`: FeedId, Live, Random, Recent, RecentNew

### 백그라운드 에피소드 동기화
WorkManager + HiltWorker 기반 3시간 주기 동기화:
1. `EpisodeSyncScheduler`가 `PeriodicWorkRequest`로 주기적 동기화 예약
2. `EpisodeSyncWorker`가 `SyncNewEpisodesUseCase`를 실행하여 팔로우 팟캐스트의 새 에피소드 확인
3. 새 에피소드 발견 시 `EpisodeSyncNotificationHelper`로 썸네일 포함 알림 표시
4. Coil로 에피소드 이미지를 Bitmap 변환 후 `setLargeIcon`으로 알림에 포함

**관련 파일:** `:app` 모듈의 `sync/` 패키지 (EpisodeSyncScheduler, EpisodeSyncWorker, EpisodeSyncNotificationHelper)

### 듀얼 플레이어 시스템
Hilt `@Player` qualifier로 ExoPlayer 인스턴스 두 개 관리:
- `@Player(EpisodivePlayers.Main)` — 전체 에피소드 재생
- `@Player(EpisodivePlayers.Clip)` — 사운드바이트/클립 재생

## 중요 구현 세부사항

### 1. Enum 처리 (필수)

모든 Enum은 **enum name이 아닌 value 프로퍼티**를 사용합니다:

```kotlin
enum class Medium(val value: String) { PODCAST("podcast"), MUSIC("music"), ... }
```

**변환** — 반드시 `entries.find()` 사용, `valueOf()` 절대 금지:
```kotlin
fun String.toMedium(): Medium? = Medium.entries.find { it.value == this }
```

Room TypeConverter도 `value`로 저장:
```kotlin
@TypeConverter fun fromMedium(medium: Medium?): String? = medium?.value
@TypeConverter fun toMedium(value: String?): Medium? = value?.toMedium()
```

**이유**: API는 소문자(`"podcast"`)를 반환하지만 enum name은 대문자(`PODCAST`)입니다. `valueOf()`를 쓰면 예외가 발생합니다.

### 2. 데이터베이스 스키마 (Room v8)

**Entity (12개):** PodcastEntity, EpisodeEntity, FeedEntity, SoundbiteEntity, FollowedPodcastEntity, LikedEpisodeEntity, PlayedEpisodeEntity, PodcastGroupEntity, EpisodeGroupEntity, PodcastFtsEntity, EpisodeFtsEntity, RecentSearchEntity

**View (2개):** PodcastWithExtrasView, EpisodeWithExtrasView

**DAO (5개):** PodcastDao, EpisodeDao, FeedDao, SoundbiteDao, RecentSearchDao

**Auto-migration:** 버전 1→8, 필요한 경우 spec 클래스 포함

모든 Entity에는 캐시 무효화를 위한 `cachedAt: Instant`와 그룹 키가 있습니다.

### 3. API 인증 (Podcast Index)

`EpisodiveInterceptor`가 헤더를 추가합니다:
- `X-Auth-Date`: 유닉스 타임스탬프
- `X-Auth-Key`: API 키
- `Authorization`: SHA-1(`apiKey + apiSecret + timestamp`)

API 인터페이스 (5개): `ChapterApi`, `EpisodeApi`, `FeedApi`, `PodcastApi`, `SoundbiteApi`

### 4. 응답 래퍼

`ResponseListWrapper<T>`는 엔드포인트별로 다른 JSON 필드명(`feeds`, `items`, `channels`)을 통합 처리합니다.

## 개발 워크플로우

### 데이터베이스 변경 시
1. Entity 수정 + DB 버전 증가
2. DAO 쿼리 수정
3. TypeConverter 추가/수정 (`entries.find` 패턴 사용)
4. 필요 시 DB View 수정
5. `:core:data`의 mapper 수정
6. 필요 시 auto-migration spec 추가
7. `RoomDatabaseRule`로 테스트 작성

### 새 API 엔드포인트 추가 시
1. `:core:network/model`에 응답 모델 추가
2. `:core:network/api`의 API 인터페이스에 추가
3. RemoteDataSource 구현 (인터페이스 + 구현체)
4. `:core:data`의 Repository 구현 업데이트
5. `:core:domain`에 Use Case 생성
6. MVI action/state로 ViewModel에 연결

### 새 Feature 추가 시
1. `episodive.android.feature` 플러그인으로 feature 모듈 생성
2. `settings.gradle.kts`에 추가
3. State/Action/Effect sealed interface + ViewModel 구현
4. state/effect를 수집하는 Composable 화면 작성
5. `:app` 모듈에서 네비게이션 연결

## 테스트

### 데이터베이스 테스트
```kotlin
@RunWith(RobolectricTestRunner::class)
class MyDaoTest {
    @get:Rule val mainDispatcherRule = MainDispatcherRule()
    @get:Rule val databaseRule = RoomDatabaseRule()
    // :core:testing의 PodcastTestData/EpisodeTestData 사용
    // Flow 검증은 Turbine의 .test { } 사용
}
```

### 테스트 데이터 (`:core:testing`)
- `PodcastTestData.podcasts` — 샘플 팟캐스트 Entity 10개
- `EpisodeTestData.episodes` — 샘플 에피소드 Entity 10개
- `FeedTestData` — 트렌딩/최근/사운드바이트 피드
- `ChannelTestData` — 채널/카테고리 데이터

**규칙:** 항상 테스트 데이터 팩토리 사용. Flow 테스트는 Turbine 사용. 인라인 테스트 객체 생성 금지.

## CLI 도구 사용 가이드

Android 관련 작업은 **`android` (Antigravity CLI, `/usr/local/bin/android`) 를 우선 사용**한다. low-level QEMU/SDK 옵션이 필요할 때만 native `emulator`/`adb`/`sdkmanager` 로 폴백.

### 매핑 (선호 → 폴백)
| 작업 | 우선 (`android`) | 폴백 (native) |
|:----|:----|:----|
| AVD 목록 | `android emulator list` | `emulator -list-avds` |
| 에뮬 부팅 (준비 완료까지 대기 포함) | `android emulator start <AVD>` | `emulator -avd <AVD> [flags...] &` |
| 에뮬 종료 | `android emulator stop <AVD>` | `adb emu kill` |
| AVD 생성/삭제 | `android emulator create` / `remove` | `avdmanager create/delete avd` |
| APK 배포·실행 | `android run --apks app.apk --activity=...` | `adb install -r app.apk && adb shell am start ...` |
| 스크린샷 | `android screen capture -o /tmp/x.png` | `adb exec-out screencap -p > /tmp/x.png` |
| UI 트리 inspect (스크린샷보다 빠름) | `android layout -p` | `adb shell uiautomator dump` |
| SDK 패키지 관리 | `android sdk install/list/update` | `sdkmanager` |
| 환경 정보 | `android info` | `echo $ANDROID_HOME` |
| 공식 문서 검색 | `android docs search <keyword>` | (수동 web 검색) |

### native 만 가능한 케이스 (폴백 필수)
- 에뮬 audio/gpu/cpu/memory 등 QEMU 옵션 (`-no-audio`, `-gpu host`, `-cores`, `-memory`)
- `-no-snapshot-load` 등 부팅 모드 세부 제어
- `adb shell` 직접 명령 (`am broadcast`, `dumpsys`, `appwidget grantbind`, `settings put`, `input keyevent` 등)
- `adb logcat` 스트리밍/필터
- WorkManager/MediaSession 같은 시스템 서비스 dumpsys

### 일반 원칙
1. 일반 부팅·배포·스크린샷은 `android` 사용 (자동 대기 + 안전한 default)
2. QEMU 플래그가 필요하면 그때만 `emulator -avd ...` 직접 실행
3. ADB shell 시스템 명령은 `adb` 직접 (래퍼 없음)
4. 에뮬을 `emulator -no-audio` 같은 플래그로 띄운 뒤에는 audio/animation 등 OS 레벨 동작 누락 가능 — 사운드/애니 검증 필요한 작업이면 `android emulator start` 로 깨끗이 부팅

## mccm:Commit Conventions

- language: 한글
- title-format: {제목}
- title-max-length: 50
- body: 선택
- branch-prefixes: feat/, fix/, refactor/, test/, docs/, chore/, ci/
- branch-format: {prefix}/{english-slug}

## mccm:PR Conventions

- language: 한글
- title-format: {타입}: {제목}
- types: feat, fix, refactor, test, docs, chore, ci
- title-max-length: 70
- body-format: ## 변경 사항\n- ...\n\n## 테스트\n...
- label-map: feat→feature, fix→bugfix, refactor→refactoring, test→test, docs→documentation, chore→chore, ci→ci/cd
- auto-assignee: true

## mccm:Cleanup Conventions

- default-branch: main
- protected-branches: main, master

## 코드 편집 규칙

**필수: Import 순서 규칙**
- 반드시 실제 코드를 먼저 수정/추가한 후, import를 마지막에 추가
- 코드 작성 전에 import를 추가하지 않음
- lint 충돌 방지를 위한 규칙

## 공통 패턴

### Coroutine Dispatcher
```kotlin
@Dispatcher(EpisodiveDispatchers.IO) val ioDispatcher: CoroutineDispatcher
// Repository의 IO 작업에는 withContext(ioDispatcher) 사용
```

### StateFlow vs SharedFlow
- `StateFlow` — UI 상태 (항상 현재 값 유지)
- `SharedFlow` — 일회성 이펙트 (네비게이션, 토스트)

### 네비게이션
- Single Activity, Compose Navigation
- 하단 바: Home, Search, Library, Clip
- `TYPESAFE_PROJECT_ACCESSORS`로 타입 안전 라우트 사용
