# CLAUDE.md

이 파일은 Claude Code(claude.ai/code)가 이 저장소에서 작업할 때 참고하는 가이드입니다.

## 프로젝트 개요

Episodive는 Kotlin과 Jetpack Compose로 만든 Android 팟캐스트 앱으로, Podcast Index API를 사용합니다. Clean Architecture + MVI 패턴, Gradle 컨벤션 플러그인 기반 멀티모듈 구조(21개 모듈), Room DB 중심의 Offline-First 설계를 따릅니다.

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
./gradlew koverXmlReportDebug            # 커버리지 리포트 생성 (Kover)
```

### git worktree 작업 시 필수: local.properties 복사

`local.properties`는 `.gitignore` 대상이라 **worktree를 만들면 자동으로 복사되지 않는다.** 이 파일에는
SDK 경로(`sdk.dir`)뿐 아니라 **Podcast Index API 키**(`podcastIndex.apiKey`, `podcastIndex.secretKey`)가 들어
있고, `core/network/build.gradle.kts`가 이 키들을 컴파일 타임에 `BuildConfig.API_KEY` / `BuildConfig.SECRET_KEY`로
굽는다. 누락하면 빌드는 되지만 런타임에 키가 비어 **모든 API 호출이 HTTP 401**로 실패한다.

worktree에서 작업·빌드하기 전에 루트의 `local.properties`를 복사할 것:

```bash
cp <repo-root>/local.properties <worktree-dir>/local.properties
```

## 아키텍처

### 모듈 구조 (21개)

#### Core 모듈 (12개)

| 모듈 | 역할 |
|:----|:----|
| `:core:model` | 순수 Kotlin 도메인 모델 (Podcast, Episode, Category, UserData 등) |
| `:core:domain` | Repository 인터페이스 + 40개 이상의 Use Case |
| `:core:data` | Repository 구현체, RemoteUpdater 캐싱 패턴 |
| `:core:network` | Retrofit API 인터페이스 5개, EpisodiveInterceptor (SHA-1 인증) |
| `:core:database` | Room DB v8, Entity 12개, View 2개, DAO 5개 |
| `:core:datastore` | DataStore Preferences 기반 사용자 설정 관리 |
| `:core:player` | ExoPlayer 래퍼, @Player qualifier로 듀얼 플레이어(Main/Clip) |
| `:core:caption` | 라이브 자막: sherpa-onnx 온디바이스 STT, ML Kit 번역, 모델 다운로드(HuggingFace) |
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
| `episodive.android.feature` | Feature 템플릿 (library + compose + hilt + test + kover) |
| `episodive.android.room` | Room + KSP + 스키마 디렉토리 설정 |
| `episodive.android.test` | 테스트 의존성 |
| `episodive.android.application.jacoco` / `episodive.android.library.jacoco` | 커버리지 설정 (Kover 기반, plugin id는 하위 호환 위해 유지) |
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

### 재생 위치 저장 규약 (필수)

재생 위치는 **`Progress.episodeId` 를 키로만** 저장한다. 저장 경로(`PlayerViewModel` 의 progress
콜렉터, `LastPlaySnapshot`, 수면 타이머)는 `playerRepository.progress` 하나만 구독한다.

**에피소드 id 를 `nowPlaying` 같은 다른 Flow 에서 가져와 `combine` 하지 마라.** `nowPlaying` 은
Room 왕복과 `flowOn(IO)` 를 거쳐 `progress` 보다 늦게 도착한다. 전환 순간 `(이전 에피소드, 새 위치)`
쌍이 만들어지고 그대로 저장되어 **이전 에피소드의 이어듣기 지점이 사라진다.** 실제로 겪은 버그다.

`PlayerDataSourceImpl` 쪽 규약:
- progress 발행은 `publishProgress()` 를 거치고 **반드시 `episodeId` 를 함께 싣는다.** 빠뜨리면
  그 경로의 저장이 조용히 멈춘다(오염이 아니라 무저장이라 눈치채기 어렵다).
- `episodeId` 는 `_nowPlaying` 이 아니라 `currentEpisode()`(= 플레이어에 실제로 올라 있는 항목)에서
  얻는다. `rehydrate` 가 `_nowPlaying` 을 플레이어와 무관하게 바꿀 수 있다.
- media3 는 `setMediaItems` 호출 스택 안에서 transition 콜백을 **인라인 실행**한다. 목록을 통째로
  갈아끼우는 경로(`prepare`/`play(list, index)`/`playClips`)는 `isPreparing` 으로 그동안의 발행을 막고,
  끝난 뒤 확정값을 한 번만 발행한다.
- `position == 0 이면 저장하지 않는다` 류 가드를 넣지 마라. "맨 앞으로 되감기" 와 "완료 후 처음부터
  다시 듣기" 가 0 을 저장해야 정상이다. `PlayerViewModelTest` 에 이를 지키는 계약 테스트가 있다.

### 클립 길이 규약 (필수)

준비가 끝나기 전에는 `player.duration` 이 `TIME_UNSET` 이라 **에피소드 메타에서 길이를 가져와야
한다.** 그때 **클립은 `Episode.duration`(전체 길이)이 아니라 `Episode.clipPlaybackDuration` 을 쓴다.**
클립은 `ClippingConfiguration` 으로 잘라 올리므로 실제로 흐르는 길이가 다르다.

전체 길이를 실으면 준비가 끝나 `progressUpdater` 가 `player.duration` 을 읽는 순간 화면의 시간이
**에피소드 전체 길이에서 클립 길이로 튄다.** 실제로 겪은 버그다.

- 메타에서 길이를 가져오는 지점은 **예외 없이 `MediaItem.playbackDuration()` 을 거친다.**
  직접 부르는 곳은 transition 콜백·`publishStartOfPlayback`·`seekTo` 의 폴백 세 곳이고,
  목록을 갈아끼우는 `prepare`·`play(list, index)`·`playClips` 는 `publishStartOfPlayback` 을
  통해 거친다. "이 경로는 늘 일반 재생이니 괜찮다" 는 예외를 두지 마라 — 그 전제가 깨지는
  날 그 경로만 조용히 전체 길이를 싣는다.
- 판정 근거는 "클립 화면인가" 가 아니라 **`toMediaItem` 이 실제로 클리핑을 걸었는지** 그 자체다
  (`MediaItem.isClipped()`). 클립 여부를 인자로 넘겨받지 마라 — 판정이 두 곳으로 갈라진다.
  클립 플레이어 인스턴스에도 잘라 올리지 않는 경로(`addTrack` 계열)가 있고, `hasClip` 이
  거짓이면 클립으로 요청해도 클리핑이 걸리지 않는다.
- 화면용 `Episode.clipPlaybackDuration` 과 발행용 `playbackDuration()` 은 **같은 것을 다른 자리에서
  답한다**(올리기 전 / 올린 뒤). 둘이 어긋나면 그 순간 숫자가 튀므로, `toMediaItem` 의 클리핑
  조건을 바꿀 때는 `clipPlaybackDuration` 의 `hasClip` 기준도 함께 맞춘다.
- **`Episode.hasClip` 은 피드 값만으로 판정할 수 있는 것만 막는다.** 시작이 음수이거나
  `시작 + 길이` 가 넘치면 `ClippingConfiguration.Builder` 가 그 자리에서 던지고, 길이가 0 이하면
  시작=끝인 창이 올라간다. 여기에 **"피드가 말한 `duration` 보다 시작이 뒤면 뺀다" 를 더하지
  마라** — 한 번 넣었다가 되돌렸다. 막지도 못하면서 멀쩡한 것만 떨어뜨린다: 피드 길이는 실제와
  양쪽으로 어긋나서, 짧게 말하면(흔하다) 정상 사운드바이트가 클립 탭에서 **에피소드 전체 재생**
  으로 바뀌고, 길게 말하면 정작 막아야 할 창을 통과시킨다.
- **잘라낸 창의 시작이 실제 오디오 길이를 넘으면 media3 는 예외를 던지지 않는다.** (1.8.0 을
  직접 실행해 확인했다.) `ClippingMediaSource` 가 `startUs = endUs` 로 창을 접어 **길이 0** 으로
  만들고, 그 창은 재생하자마자 ENDED 가 된다. `IllegalClippingException(REASON_START_EXCEEDS_END)`
  은 **요청한** 끝이 시작보다 앞일 때만 나는데 `hasClip` 이 이미 막아 도달 불가다. 그러니
  이 경우를 두고 "클리핑이 잘못되면 재생 오류로 드러난다" 고 가정하지 마라 — **조용히 끝난
  것처럼 보인다.** (클리핑이 예외로 실패하는 길이 아주 없지는 않다. 탐색 불가 미디어에 시작
  오프셋을 걸면 `REASON_NOT_SEEKABLE_TO_START` 가 난다. 그쪽은 `onPlayerError` 로 오는데 지금은
  로그만 남기므로, 화면은 멈춘 카드가 된다.)
- 그래서 **클립의 ENDED 자동 넘김은 두 가지를 함께 본다.**
  1. `progress.position` 이 0 을 벗어났는가 — 접힌 창의 ENDED 로 넘기면 그런 항목이 이어질 때
     목록을 소리 없이 훑고 지나간다. 피드 메타로 점치지 말고 재생해 본 결과로 가른다.
  2. **끝난 그 클립이 아직 그 자리에 있는가**(`progress.episodeId`). `settledPage` 는
     `if (isScrollInProgress) settledPageState else currentPage` 인 파생값이라, 스크롤이 멎기를
     기다렸다가 읽으면 **이미 사용자가 착지한 페이지** 다. 거기서 한 칸 더 가면 사용자가 방금
     고른 클립을 건너뛴다. 탭 재진입에도 같은 조건이 쓰인다 — 플레이어가 싱글턴이라 지난번
     (ENDED, position > 0) 이 남아 있어 들어오자마자 이펙트가 돈다. 다만 재진입을 **통째로
     막지는 못한다**: 멎어 있는 페이지의 클립이 마침 그때 끝난 클립이면 정상 종료와 구분할
     근거가 없다.
  이 가드는 **`progress` 가 ENDED 순간의 스냅샷이라는 데 기댄다.** 목록·콜백처럼
  `rememberUpdatedState` 로 감싸 "최신" 을 보게 만들면, 뒤늦게 도착하는 확정 progress 로
  판정이 뒤집혀 가드가 무력해진다. 일관성을 명목으로 감싸지 마라.
  "그 사이 재생이 시작되면 이펙트가 취소된다" 에 기대지 마라. 그 취소는 요청→ViewModel→
  플레이어→StateFlow→재구성을 거쳐야 해서 대기가 풀리는 같은 스냅샷을 이기지 못한다.
- 클리핑 창을 짓는 곳은 **`Episode.clippingConfiguration(isClip)` 하나뿐이다.** `toMediaItem` 과
  `playClip` 의 "이미 올라 있는가" 판정이 둘 다 이 함수를 거친다. 판정 쪽에서 조건을 손으로
  베껴 쓰면 `hasClip` 이 거짓인 경우가 곧바로 갈라진다 — 실제로 갈라져서, 그 에피소드만 누를
  때마다 처음으로 되감겼다.
- 화면 쪽에서는 **`progress.episodeId` 로 자기 차례인지 가른 뒤** 남은 시간을 그린다. 지금 재생
  중인 클립의 `progress` 를 모든 카드가 공유하면, 아직 자기 차례가 아닌 카드가 남의 진행 시간을
  빌려 그리고 재생이 시작되는 순간 숫자가 튄다. **한 배지 안의 파도 애니메이션과 시간은 반드시
  같은 기준으로 가른다** — 애니메이션만 `currentPage` 로 가르면 그 값은 스와이프 50% 에서
  뒤집히는데 `progress` 는 페이지가 멎은 뒤 따라와, 넘기는 내내 둘이 서로 다른 말을 한다.
  `ClipScreenTest` 에 계약 테스트가 있다.

### 라이브 자막 규약 (필수)

플레이어 화면의 자막은 두 갈래다. 피드가 VTT transcript 를 주면 media3 텍스트 트랙(`cue`)을
그대로 쓰고, 없으면 **Main 플레이어의 PCM 을 `SpeechAudioTap` 으로 엿들어 sherpa-onnx 로
받아쓴다.** 자막 토글이 켜져 있으면 둘 다 기기 언어로 번역한 줄을 붙인다(ML Kit).

- **에피소드 구분은 `progress.episodeId` 로만 한다.** 재생 위치 저장 규약과 같은 이유다.
  `ObserveLiveCaptionUseCase` 는 `nowPlaying.id == progress.episodeId` 일 때만 세션을 열고,
  화면은 `LiveCaption.episodeId == progress.episodeId` 일 때만 그린다.
- **sherpa stream 하나에는 샘플레이트 하나만 넣는다.** 같은 stream 에 다른 레이트를 넣으면
  네이티브가 `exit(-1)` 로 **프로세스를 죽인다**(예외가 아니다). 그래서 탭은 flush·오버런마다
  구간(segment)을 끊고, 엔진은 구간이 바뀌면 stream 을 새로 만든다. `recognizer.reset` 으로
  재사용하지 마라 — 내부 리샘플러가 리셋되지 않는다.
- **표시 텍스트는 `result.tokens` 로 만든다. `result.text` 를 쓰지 마라.** 한국어 모델은
  `text` 에서 띄어쓰기를 잃는다('걔는괜찮은척…'). 토큰의 선행 공백·`▁` 이 단어 경계다
  (`CaptionTextNormalizer`).
- **`decodingMethod` 는 `greedy_search` 로 고정한다.** 긴 줄 강제 확정(`CaptionLineTracker`)은
  "이미 낸 토큰은 바뀌지 않는다" 에 기댄다. beam search 는 앞 토큰을 고친다.
- **자막 StateFlow(`PlayerViewModel.caption`)는 플레이어 시트만 수집한다.** 10-arity `state` 에
  끼우지 마라 — `PlayerBar` 가 `state` 를 늘 수집하므로 미니바만 보여도 인식이 돈다. 시트가
  닫히면 `WhileSubscribed(5s)` 뒤 세션이 끝나고, `RecognizerCache` 가 30초 뒤 인식기(수백 MB)를
  놓는다.
- 꺼진 동안에도 `LiveCaptionState.availability` 는 계산한다. 토글이 이 값 하나로 "켜면서
  바로 모델을 받을지" 를 정하므로, 꺼진 동안 `Unknown` 으로 뭉개면 두 번 눌러야 받기 시작한다.
- 모델 다운로드는 **caption 전용 OkHttpClient** 로 한다. `:core:network` 클라이언트에는
  Podcast Index 인증 인터셉터가 있어 키가 HuggingFace 로 샌다. 파일은 revision·sha256 고정
  (`CaptionModelRegistry`), `.part` → 검증 → rename 이다.
- **표시는 앨범아트 전체를 쓰는 롤링이다**(`RollingCaption.kt`). 번역 세션이면 위(원문)/아래(번역)로
  반씩 나눈다. 나눌지는 `LiveCaption.isTranslating` 으로 정한다 — 번역 줄이 있는지로 정하면 첫
  번역이 오는 순간 원문 영역이 반으로 줄며 튄다.
  - 롤링 이동량은 **앵커 줄**(이전·현재에 모두 있는 가장 최근 줄)이 바닥에서 멀어진 거리로 잡고,
    `Layout` 측정 단계에서 바로 반영한다. "콘텐츠 전체 높이가 늘면 애니메이션" 으로 되돌리지 마라 —
    목록이 차서 맨 윗줄이 버려지는 만큼 높이가 빠져 새 줄이 제자리에서 툭 바뀐다. `LazyColumn` 도
    쓰지 마라 — 뷰포트 밖 아이템을 즉시 dispose 해 윗줄이 한 프레임에 사라진다. 둘 다 실기기에서 겪었다.
  - **화면은 줄이 아니라 발화 문단 단위로 그린다**(`captionParagraphs`). 인식기는 긴 발화를
    `maxLineChars` 에서 여러 줄로 끊지만(번역을 발화 도중에 받기 위해서다), 같은 `utteranceId` 의
    연속한 줄은 한 `Text` 로 이어 그린다. 줄마다 블록으로 그리면 줄바꿈이 영역 폭이 아니라 글자 수에서
    일어나 조각마다 한두 단어짜리 짧은 줄이 남는다(실기기에서 겪었다). 줄바꿈은 영역 폭과 발화 경계
    (말이 멈춘 곳)에서만 일어난다 — 발화 경계를 없애 전부 잇는 안은 사용자가 택하지 않았다.
    문단 스타일은 `LineBreak.Simple` 로 고정한다. 균형 맞춤 줄바꿈은 뒤에 단어가 붙을 때 앞 줄
    끝 단어를 끌어내려 문단 전체가 출렁인다. 롤링 앵커도 문단(`utteranceId`) 단위다.
  - `CueOverlay` 는 롤링 오버레이와 `PushUpCue` 를 **분기 없이 늘 부른다.** if/else 로 번갈아
    부르면 `AnimatedVisibility` 가 컴포지션에서 빠져 페이드가 사라진다.
  - `CaptionPresenter.clear()` 는 지운 구간의 마지막 줄 id 를 번역 하한으로 남긴다. 번역은 따로
    launch 된 코루틴이라 시크로 취소되지 않아, 하한이 없으면 시크 전 문장의 번역이 새 구간에 붙는다.
    빈 partial 은 줄로 싣지 않는다(빈 `Text` 도 한 줄 높이를 차지한다).
  - VTT 는 빈 cue(cue 사이 공백)로 비우지 않고 **시크(`PlayerRepository.seeks`)로만 비운다.** 빈
    cue 로 비우면 cue 가 바뀔 때마다 커버 전체가 비었다 다시 찬다. cue 번역은 cue 마다 따로 띄운다
    (`collectLatest` 로 앞 번역을 취소하면 원문은 남고 번역만 빠진다).
  - STT 도 구간이 바뀔 때마다 비우지 않는다. **flush(시크·EOS·리셋)로 열린 구간만 비우고,
    오버런(`PcmChunk.isContinuation`)은 stream·tracker 만 새로 열고 흘러가던 줄을 확정으로 남긴다.**
    저사양 기기에서 인식이 실시간을 못 따라가면 링(약 11초)이 넘칠 때마다 커버 전체가 사라진다.
- 네이티브 `.so`(onnxruntime, ML Kit)는 APK 에 들어 있어야 한다. Play 는 스토어 밖에서 받은
  실행 코드를 금지한다. 런타임에 받는 것은 모델(데이터)뿐이다. APK 는 ABI 별로 나눠
  (`splits.abi`) 릴리즈도 ABI 별로 올린다(`publish.yml`).

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

### 2. 데이터베이스 스키마 (Room v12)

**Entity (13개):** PodcastEntity, EpisodeEntity, FeedEntity, SoundbiteEntity, FollowedPodcastEntity, LikedEpisodeEntity, PlayedEpisodeEntity, SavedEpisodeEntity, PodcastGroupEntity, EpisodeGroupEntity, PodcastFtsEntity, EpisodeFtsEntity, RecentSearchEntity

**View (2개):** PodcastWithExtrasView, EpisodeWithExtrasView

**DAO (5개):** PodcastDao, EpisodeDao, FeedDao, SoundbiteDao, RecentSearchDao

**마이그레이션:** 1→8은 auto-migration(필요 시 spec 클래스), 8→12는 `migration/` 의 수동
`Migration` 객체이며 `DatabaseModule` 에서 `addMigrations` 로 등록합니다.

**캐시 표의 정렬:** `feeds`·`soundbites` 는 원격이 준 순위를 `sortOrder` 에 담고 모든 쿼리가
그 컬럼으로 정렬합니다. `LIMIT`/`OFFSET` 페이징은 정렬이 정해져야만 성립하므로 새 쿼리를 더할
때도 `ORDER BY` 를 빠뜨리지 않습니다. `feeds` 의 기본키는 `(id, groupKey)` 복합키로, 같은 피드가
여러 목록에 동시에 속할 수 있습니다.

모든 Entity에는 캐시 무효화를 위한 `cachedAt: Instant`와 그룹 키가 있습니다.

**스키마를 바꾼 뒤에는** `./gradlew :core:database:kspDebugKotlin` 으로 `schemas/…/<버전>.json` 을
내보내고, 그 `createSql` 을 그대로 옮겨 마이그레이션을 씁니다. `Migration11to12Test` 가 그 방식의
본보기입니다 — 내보낸 스키마로 이전 버전 DB를 만들고 Room 이 여는 순간의 검증에 판정을 맡깁니다.

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

**예외는 `:core:model` 의 테스트 하나뿐이다.** `:core:testing` 은 `episodive.android.library`
플러그인을 써서 산출물이 AAR 인데, `:core:model` 은 `episodive.jvm.library` 라 그 AAR 을 소비할
수 없다. (Gradle 의존 순환은 아니다 — `testImplementation` 방향은 태스크 그래프상 순환이 아니다.
막는 것은 산출물 형식이다.) 그래서 `:core:model` 의 테스트는 자기 파일 안에 최소한의 로컬
팩토리를 두고 그 사유를 파일 머리에 적는다. 팩토리를 함께 쓰고 싶다면 순수 JVM 모듈로 떼어내는
것이 길이지, 이 예외를 다른 모듈로 넓히는 것이 아니다.

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
- body-format: ## 변경 사항\n- ...\n\n## 테스트\n...\n\n## 리뷰\n- [ ] Claude Code 코드 리뷰 (`/code-review`)
- label-map: feat→feature, fix→bugfix, refactor→refactoring, test→test, docs→documentation, chore→chore, ci→ci/cd
- auto-assignee: true

### PR 전에 코드 리뷰를 돌린다 (필수)

`/pr` 로 PR 을 만들기 **전에** `/code-review` 를 돌리고, 나온 지적을 반영한 뒤 본문의
`## 리뷰` 체크박스를 `- [x]` 로 바꾼다. 리뷰를 돌리지 않았으면 체크하지 않은 채로 두거나
PR 을 draft 로 연다 — **체크가 비어 있는 것 자체가 "아직 리뷰를 안 거쳤다"는 신호**이므로
형식만 맞추려고 미리 체크하지 마라.

**빌드·테스트·기기 확인은 리뷰를 대신하지 못한다.** 그것들은 작성자가 자기 코드를 자기
기준으로 확인한 것이라, 작성자가 놓친 전제는 그대로 통과한다. `/code-review` 는 diff 를
별도 패스로 훑어 정확성·경계조건·리소스 수명·중복을 본다.

`/pr` 스킬 자체에는 리뷰 단계가 없다(스킬 단계는 사전 확인 → push → 제목 → 본문 → 생성 →
업데이트 → Actions 체크 추적 → 보고). 그러니 **이 규칙을 지키는 것은 스킬이 아니라 여기를
읽는 쪽이다.** 실제로 한 번 건너뛰어 PR 을 올렸다가 draft 로 되돌린 적이 있다(#110).

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

## graphify

The knowledge graph at graphify-out/ — god nodes, community structure, cross-file relationships — is
**local-only and not committed**. Generate it once per clone with `graphify update .` (AST-only, no API
cost); the rules below apply only once it exists. See `## graphify 로컬 셋업`.

Rules:
- For codebase questions, first run `graphify query "<question>"` when graphify-out/graph.json exists. Use `graphify path "<A>" "<B>"` for relationships and `graphify explain "<concept>"` for focused concepts. These return a scoped subgraph, usually much smaller than GRAPH_REPORT.md or raw grep output.
- If graphify-out/wiki/index.md exists, use it for broad navigation instead of raw source browsing.
- Read graphify-out/GRAPH_REPORT.md only for broad architecture review or when query/path/explain do not surface enough context.
- After modifying code, run `graphify update .` to keep the graph current (AST-only, no API cost).

## Codebase Analysis Policy

- 코드 구조, 함수 관계, 호출 흐름, 의존성 파악 시 반드시 graphify `query` / `explain` / `path`를 먼저 사용한다.
- Read/Grep으로 여러 파일을 직접 순회하기 전에 graphify로 먼저 조회한다.
- 조회 결과가 불충분하거나 최신 변경이 의심될 때만 Read/Grep으로 fallback한다.
- 구조적 변경 시 커밋 전 `/graphify --update`를 실행한다.

## graphify 로컬 셋업

**graphify 산출물(`graphify-out/`)은 커밋하지 않는다.** 28MB짜리 자동 생성물(그중 `graph.html`이
4.7MB)이라 PR diff를 덮고 브랜치마다 충돌한다. `.gitignore` 대상이며 **각자 로컬에서 생성**한다.

**Claude PreToolUse 가드는 `.claude/settings.json`에 커밋되어 있다.** clone하면 그대로 적용되므로
설정할 것이 없다. 반면 **그래프와 git 훅은 로컬 자산**이므로(훅은 `.git/hooks`에 있어 버전 관리가 안 된다)
각 기여자가 **clone 후 한 번** 직접 만든다.

```bash
graphify install                # graphify 0.9.12 (버전 고정 — 아래 Grep 가드가 이 버전 동작에 기댄다)
graphify update .               # 그래프 최초 생성 (AST-only, API 비용 없음). 이후 갱신도 같은 명령
graphify hook install           # post-commit·post-checkout 훅 설치
rm -f .git/hooks/post-checkout  # post-checkout 훅은 제거한다 (아래 참고)
```

**`graphify claude install`은 실행하지 않는다.** 이 명령은 `.claude/settings.json`을 자기 형식으로
덮어써서, 커밋된 설정의 두 가지 보완(머신 독립 경로, 아래 Grep 가드)을 날려버린다. 이미 실행했다면
`git checkout -- .claude/settings.json`으로 되돌린다.

graphify가 PATH에 없어도 훅은 조용히 통과하고, 가드는 `graphify-out/graph.json`이 없으면 발화하지
않는다. graphify를 설치하지 않은 기여자도 작업에 지장이 없다.

### PreToolUse 가드 구성

| 매처 | 실행 대상 | 비고 |
|:----|:----|:----|
| `Bash` | `graphify hook-guard search` | `command` 필드에서 grep류 명령을 감지 |
| `Read\|Glob` | `graphify hook-guard read` | 입력 문자열의 파일 확장자를 감지 |
| `Grep` | `scripts/graphify-grep-guard.sh` | 저장소 자체 스크립트 |

`Grep`만 자체 스크립트인 이유: `graphify hook-guard`(0.9.12)는 tool_input에서 **파일 확장자를 찾아낼
때만** 발화한다. Grep 툴의 입력은 `{"pattern":"class EpisodeDao","path":"core"}` 형태라 확장자가 없어
`search`·`read` 어느 쪽도 반응하지 않는다(실측 확인, `glob` 파라미터를 붙여도 동일). Grep은 그 자체가
코드 검색이므로 자체 스크립트가 패턴과 무관하게 항상 안내한다.

### 동작 규칙

- post-commit 훅은 커밋 **후** `git diff HEAD~1`로 바뀐 **코드** 파일만 감지해 그래프를 재생성한다. 산출물이 무시 대상이라 재생성돼도 작업 트리는 깨끗하게 유지된다 — 별도 커밋할 것이 없다.
- 문서/이미지만 변경한 경우 훅이 무시하므로 그때만 `graphify update .`(또는 `/graphify --update`)를 수동 실행한다.
- **post-checkout 훅은 제거한다.** `graphify hook install`이 함께 설치하지만, 이 훅은 브랜치 전환마다 **전체 재빌드**를 돌리고 `GRAPHIFY_SKIP_HOOK`을 무시한다. post-commit 훅만으로 충분하다. (그래프가 누적으로 부풀면 `graphify update . --force`로 리셋 가능.)
- 그래프는 로컬 전용이므로 브랜치를 오가면 현재 체크아웃과 어긋날 수 있다. 조회 결과가 최신 코드와 다르면 `graphify update .`로 갱신한 뒤 다시 조회한다.
