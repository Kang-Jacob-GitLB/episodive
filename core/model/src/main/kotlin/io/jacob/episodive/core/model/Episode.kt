package io.jacob.episodive.core.model

import io.jacob.episodive.core.model.mapper.toIntSeconds
import kotlin.time.Duration
import kotlin.time.Instant

data class Episode(
    val id: Long,
    val title: String,
    val link: String,
    val description: String? = null,
    val guid: String,
    val datePublished: Instant,
    val dateCrawled: Instant,
    val enclosureUrl: String,
    val enclosureType: String,
    val enclosureLength: Long, // in bytes, 0 is live
    val startTime: Instant? = null, // for live episodes
    val endTime: Instant? = null, // for live episodes
    val status: String? = null, // for live episodes
    val contentLink: String? = null, // for live episodes
    val duration: Duration? = null,
    val explicit: Boolean,
    val episode: Int? = null,
    val episodeType: EpisodeType? = null,
    val season: Int? = null,
    val image: String,
    val feedItunesId: Long? = null,
    val feedImage: String,
    val feedId: Long,
    val feedUrl: String? = null,
    val feedAuthor: String? = null,
    val feedTitle: String? = null,
    val feedLanguage: String,
    val categories: List<Category> = emptyList(),
    val chaptersUrl: String? = null,
    val transcriptUrl: String? = null,
    val likedAt: Instant? = null,
    val playedAt: Instant? = null,
    val position: Duration = Duration.ZERO,
    val isCompleted: Boolean = false,
    val clipStartTime: Instant? = null, // for soundbite
    val clipDuration: Duration? = null, // for soundbite
    val savedAt: Instant? = null,
    val filePath: String? = null,
    val downloadStatus: DownloadStatus? = null,
    val downloadProgress: Float = 0f,
) {
    val isLiked: Boolean = likedAt != null

    val isSaved: Boolean = savedAt != null

    val isDownloading: Boolean
        get() = downloadStatus == DownloadStatus.PENDING ||
                downloadStatus == DownloadStatus.DOWNLOADING ||
                downloadStatus == DownloadStatus.PAUSED

    val isDownloaded: Boolean = downloadStatus == DownloadStatus.COMPLETED

    val progress: Float = duration?.let {
        if (it == Duration.ZERO) 0f
        else position.toIntSeconds().toFloat() / it.toIntSeconds()
    } ?: 0f

    val remain: Duration? = duration?.let {
        if (it == Duration.ZERO) null
        else it - position
    }

    val clipStartPositionMs: Long = clipStartTime?.toEpochMilliseconds() ?: 0L
    val clipEndPositionMs: Long = clipStartPositionMs + (clipDuration?.inWholeMilliseconds ?: 0L)
    /**
     * 클립으로 재생할 수 있는가. 플레이어는 이 값이 참일 때만 미디어 아이템을 잘라 올린다.
     *
     * 피드가 주는 값을 그대로 믿지 않고 두 가지를 함께 본다. 표시 길이만 손보는 것으로는
     * 막을 수 없다 — 잘라 올리는 판단 자체가 여기서 갈리기 때문이다.
     *
     * - 길이가 0 이하면 시작=끝인 창이 올라가 재생하자마자 ENDED 가 되고, 그 ENDED 가 다음
     *   페이지로 넘기는 것을 연쇄시켜 목록을 소리 없이 훑고 지나간다.
     * - 시작이 음수면 `ClippingConfiguration.Builder.setStartPositionMs` 가 그 자리에서
     *   IllegalArgumentException 을 던진다(media3 가 `>= 0` 을 단언한다). 즉 크래시다.
     * - 시작이 터무니없이 크면 `시작 + 길이` 가 Long 을 넘겨 끝이 음수가 된다. 그런 값이
     *   그대로 올라가면 `setStartPositionMs` 가 던진다 — media3 를 실행해 확인한 것인데,
     *   이유가 조금 뜻밖이다. `Util.msToUs` 는 센티널 둘(TIME_UNSET·TIME_END_OF_SOURCE)만
     *   통과시키고 나머지는 **포화 없이 1000 을 곱하므로**, 거대한 양수 시작이 마이크로초로
     *   내려가며 도로 음수로 감긴다. 위의 `clipStartPositionMs >= 0` 은 그것을 잡지 못한다.
     *   실제로 막는 것은 이 조항(`끝 > 시작`)이다.
     *
     * 마지막 조건이 `>` 인 것은 넘침만 잡으려는 것이 아니다. 밀리초로 내리면 0 이 되는 길이
     * (예: 500µs)가 `isPositive()` 를 통과해 시작=끝인 창을 만들 수 있는데, 그것이 바로 위의
     * 첫 항목이 막으려는 상태다.
     *
     * **시작이 실제 오디오 끝을 넘는 경우는 여기서 막지 않는다.** 한 번 막아 봤다가 되돌린
     * 판단이라 이유를 남긴다.
     *
     * 먼저 사실관계부터 — media3 1.8.0 을 직접 돌려 확인한 것이다.
     * `ClippingMediaSource` 는 이때 **예외를 던지지 않는다.** 창의 끝을 실제 길이로 자른 뒤
     * `if (startUs > endUs) startUs = endUs` 로 시작까지 접어, 길이 0 인 창을 만든다.
     * `IllegalClippingException(REASON_START_EXCEEDS_END)` 은 **요청한** 끝이 시작보다 앞일
     * 때만 나는데, 그 조합은 바로 위 `clipEndPositionMs > clipStartPositionMs` 가 이미 막아
     * 여기서는 도달할 수 없다.
     *
     * 그래서 실제 결과는 재생 실패가 아니라 **즉시 ENDED** 다. 그리고 클립 화면의 자동 넘김이
     * 그 ENDED 에 반응하면 목록을 소리 없이 훑고 지나간다 — 맨 위 첫 항목이 막으려는 것과
     * 같은 상태다.
     *
     * 그럼에도 여기서 막지 않는 것은, 여기서 견줄 수 있는 값이 [duration] 뿐이기 때문이다.
     * 그것은 **피드가 말한** 길이라 실제와 양쪽으로 어긋난다. 짧게 말하면(흔하다,
     * [clipPlaybackDuration] 참고) 멀쩡한 사운드바이트를 떨어뜨려 클립 탭이 에피소드 전체를
     * 틀게 하고, 길게 말하면 정작 접히는 창을 그대로 통과시킨다. **막지도 못하면서 멀쩡한
     * 것만 떨어뜨리는 조건이다.**
     *
     * 접힌 창에 대한 방어는 `ClipScreen` 의 ENDED 자동 넘김에 있다 — 피드가 뭐라 했든
     * **재생해 본 결과**(position 이 0 을 벗어났는가)로 가르므로 피드 메타와 어긋날 여지가
     * 없다. 다만 완전한 방어는 아니다 — progressUpdater 의 0.5초 격자보다 짧은 재생 창은
     * 정상인데도 position 이 0 인 채로 판정될 수 있다. 그때는 넘기지 않는 쪽으로 실패하는데,
     * 한 항목을 건너뛰지 않는 정도가 아니라 **그 자리에 멈춘다** — 사용자가 스와이프하거나
     * 카드를 눌러 재생을 다시 걸기 전까지 자동 넘김이 이어지지 않는다.
     */
    val hasClip: Boolean = clipStartTime != null &&
            clipDuration != null &&
            clipDuration.isPositive() &&
            clipStartPositionMs >= 0L &&
            clipEndPositionMs > clipStartPositionMs

    /**
     * 클립으로 재생할 때 흐르는 길이 — **아직 플레이어에 올리기 전인 화면**이 쓴다.
     *
     * 플레이어는 [hasClip] 일 때만 잘라 올리므로 그 판정을 그대로 따른다. 이 기준이
     * `toMediaItem` 의 클리핑 조건과 어긋나면 카드의 시간과 플레이어가 발행하는 길이가
     * 갈라져, 재생이 준비되는 순간 숫자가 튄다.
     *
     * 올린 **뒤**의 길이는 이 값을 쓰지 않는다. 그쪽은 실제로 걸린 클리핑 창을 직접 재므로
     * (`PlayerDataSourceImpl.playbackDuration`) 추정이 필요 없다. 두 자리가 답하는 질문이
     * 다르다 — 여기는 "올리면 얼마가 될까", 저기는 "올라간 것이 얼마인가".
     *
     * 클립이 에피소드 끝을 넘기는 경우는 손대지 않는다. media3 는 실제 미디어 길이로 자르는데
     * 여기서 쓸 수 있는 것은 피드가 말한 [duration] 뿐이고, 그 둘은 자주 어긋난다. 피드가
     * 실제보다 짧게 말하면(흔하다) 상한이 실제보다 더 깎아, 준비가 끝나는 순간 표시가 도로
     * 늘어난다 — 줄어드는 튐을 늘어나는 튐으로 바꿀 뿐이다.
     */
    val clipPlaybackDuration: Duration
        get() = (if (hasClip) clipDuration else duration) ?: Duration.ZERO
}
