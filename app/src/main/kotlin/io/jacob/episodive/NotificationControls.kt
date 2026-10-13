package io.jacob.episodive

import io.jacob.episodive.core.model.Station

/**
 * 알림·위젯 컨트롤이 지금 무엇을 허용하는지.
 *
 * @property showCustomButtons 건너뛰기·좋아요 버튼과 그 명령을 받을지. 라이브 스테이션에는 없다.
 * @property isLiked 좋아요 아이콘이 채워졌는지. 버튼이 보일 때만 뜻이 있다.
 */
internal data class NotificationControls(
    val showCustomButtons: Boolean,
    val isLiked: Boolean,
)

/**
 * 서비스가 들고 있는 두 필드(스테이션·에피소드의 좋아요)에서 컨트롤을 정한다. 판단을 서비스 밖으로
 * 빼 두어야 스테이션이 먼저 올라온 순서까지 단위 테스트로 지킬 수 있다.
 */
internal fun notificationControls(station: Station?, isLiked: Boolean): NotificationControls =
    NotificationControls(
        showCustomButtons = station == null,
        isLiked = station == null && isLiked,
    )
