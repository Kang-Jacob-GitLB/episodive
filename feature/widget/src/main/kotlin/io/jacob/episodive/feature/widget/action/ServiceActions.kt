package io.jacob.episodive.feature.widget.action

import android.app.ActivityManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent

/**
 * Widget 에서 MediaNotificationService / MainActivity 를 호출하기 위한 helper.
 *
 * `:feature:widget` 은 `:app` 에 의존할 수 없으므로
 * [setClassName] + action 문자열 하드코딩으로 역의존을 회피한다.
 *
 * 상수는 다음 파일과 동기화되어야 한다:
 *   - `app/.../MediaNotificationService.kt` companion (ACTION_WIDGET_*)
 *   - `app/.../MainActivity.kt` companion (EXTRA_WIDGET_AUTOPLAY)
 */
private const val APP_PACKAGE = "io.jacob.episodive"
private const val SERVICE_CLASS = "io.jacob.episodive.MediaNotificationService"
private const val ACTIVITY_CLASS = "io.jacob.episodive.MainActivity"

const val ACTION_WIDGET_PLAY_PAUSE = "io.jacob.episodive.action.WIDGET_PLAY_PAUSE"
const val ACTION_WIDGET_SEEK_FWD = "io.jacob.episodive.action.WIDGET_SEEK_FWD"
const val ACTION_WIDGET_SEEK_BWD = "io.jacob.episodive.action.WIDGET_SEEK_BWD"
const val EXTRA_WIDGET_AUTOPLAY = "widget_autoplay"

private fun serviceIntent(context: Context, action: String): Intent =
    Intent().apply {
        setClassName(context.packageName.ifEmpty { APP_PACKAGE }, SERVICE_CLASS)
        this.action = action
    }

private fun activityAutoplayIntent(context: Context): Intent =
    Intent().apply {
        setClassName(context.packageName.ifEmpty { APP_PACKAGE }, ACTIVITY_CLASS)
        putExtra(EXTRA_WIDGET_AUTOPLAY, true)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
    }

private val pendingFlags =
    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT

/**
 * Play/Pause 는 서비스가 실행 중이면 `getService`, 아니면 MainActivity cold-start + autoplay 로 fallback.
 */
fun playPausePendingIntent(context: Context, requestCode: Int): PendingIntent {
    return if (isMediaServiceRunning(context)) {
        PendingIntent.getService(
            context,
            requestCode,
            serviceIntent(context, ACTION_WIDGET_PLAY_PAUSE),
            pendingFlags,
        )
    } else {
        PendingIntent.getActivity(
            context,
            requestCode,
            activityAutoplayIntent(context),
            pendingFlags,
        )
    }
}

/**
 * Seek 은 서비스가 살아 있을 때만 의미가 있으므로 동일 fallback 정책을 공유한다.
 * (cold 상태에서 눌러도 autoplay 로 재생을 시작하는 편이 사용자 친화적)
 */
fun seekForwardPendingIntent(context: Context, requestCode: Int): PendingIntent =
    seekPendingIntent(context, requestCode, ACTION_WIDGET_SEEK_FWD)

fun seekBackwardPendingIntent(context: Context, requestCode: Int): PendingIntent =
    seekPendingIntent(context, requestCode, ACTION_WIDGET_SEEK_BWD)

private fun seekPendingIntent(
    context: Context,
    requestCode: Int,
    action: String,
): PendingIntent {
    return if (isMediaServiceRunning(context)) {
        PendingIntent.getService(
            context,
            requestCode,
            serviceIntent(context, action),
            pendingFlags,
        )
    } else {
        PendingIntent.getActivity(
            context,
            requestCode,
            activityAutoplayIntent(context),
            pendingFlags,
        )
    }
}

/**
 * MainActivity 를 연 뒤 현재 재생 화면으로 이동하는 용도의 PendingIntent.
 * (탭 시 열림, autoplay 없음)
 */
fun openAppPendingIntent(context: Context, requestCode: Int): PendingIntent =
    PendingIntent.getActivity(
        context,
        requestCode,
        Intent().apply {
            setClassName(context.packageName.ifEmpty { APP_PACKAGE }, ACTIVITY_CLASS)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        },
        pendingFlags,
    )

@Suppress("DEPRECATION")
private fun isMediaServiceRunning(context: Context): Boolean {
    val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        ?: return false
    // getRunningServices 는 자기 프로세스의 서비스에 한해서는 현재도 신뢰할 수 있다.
    return runCatching {
        am.getRunningServices(Int.MAX_VALUE).any { it.service.className == SERVICE_CLASS }
    }.getOrDefault(false)
}
