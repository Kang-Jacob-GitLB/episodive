package io.jacob.episodive.core.ui

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import io.jacob.episodive.core.model.DataError

/**
 * 실패 원인을 사용자에게 보여줄 문장으로 옮긴다.
 *
 * 문구는 "무엇이 잘못됐는지"보다 **"이제 무엇을 하면 되는지"** 를 담는다. 원인을 정확히
 * 알려줘도 다음 행동을 모르면 사용자에게는 막다른 길이다.
 */
@Composable
fun DataError.asUiMessage(): String = stringResource(messageRes)

/** [asUiMessage] 의 문자열 리소스. 컴포지션 밖(이펙트 수집 등)에서 문장을 고를 때 쓴다. */
@get:StringRes
val DataError.messageRes: Int
    get() = when (this) {
        DataError.Offline -> R.string.core_ui_error_offline
        DataError.Timeout -> R.string.core_ui_error_timeout
        DataError.Server -> R.string.core_ui_error_server
        DataError.Unauthorized -> R.string.core_ui_error_unauthorized
        DataError.NotFound -> R.string.core_ui_error_not_found
        is DataError.Unexpected -> R.string.core_ui_error_unexpected
    }
