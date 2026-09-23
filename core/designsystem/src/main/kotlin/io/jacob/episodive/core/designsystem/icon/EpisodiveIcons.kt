package io.jacob.episodive.core.designsystem.icon

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.DownloadDone
import io.jacob.episodive.core.designsystem.icon.tabler.ArrowLeft
import io.jacob.episodive.core.designsystem.icon.tabler.ArrowsDiagonal
import io.jacob.episodive.core.designsystem.icon.tabler.ArrowsDiagonalMinimize2
import io.jacob.episodive.core.designsystem.icon.tabler.BadgeCc
import io.jacob.episodive.core.designsystem.icon.tabler.Blob
import io.jacob.episodive.core.designsystem.icon.tabler.BlobFilled
import io.jacob.episodive.core.designsystem.icon.tabler.CalendarTime
import io.jacob.episodive.core.designsystem.icon.tabler.Check
import io.jacob.episodive.core.designsystem.icon.tabler.ChevronDown
import io.jacob.episodive.core.designsystem.icon.tabler.ChevronRight
import io.jacob.episodive.core.designsystem.icon.tabler.Container
import io.jacob.episodive.core.designsystem.icon.tabler.ContainerFilled
import io.jacob.episodive.core.designsystem.icon.tabler.CreativeCommonsBy
import io.jacob.episodive.core.designsystem.icon.tabler.DeviceFloppy
import io.jacob.episodive.core.designsystem.icon.tabler.DeviceFloppyFilled
import io.jacob.episodive.core.designsystem.icon.tabler.DotsVertical
import io.jacob.episodive.core.designsystem.icon.tabler.FileExport
import io.jacob.episodive.core.designsystem.icon.tabler.FileImport
import io.jacob.episodive.core.designsystem.icon.tabler.Fountain
import io.jacob.episodive.core.designsystem.icon.tabler.FountainFilled
import io.jacob.episodive.core.designsystem.icon.tabler.HeartFilled
import io.jacob.episodive.core.designsystem.icon.tabler.HeartPlus
import io.jacob.episodive.core.designsystem.icon.tabler.History
import io.jacob.episodive.core.designsystem.icon.tabler.Label
import io.jacob.episodive.core.designsystem.icon.tabler.LetterISmall
import io.jacob.episodive.core.designsystem.icon.tabler.LetterXSmall
import io.jacob.episodive.core.designsystem.icon.tabler.List
import io.jacob.episodive.core.designsystem.icon.tabler.ListNumbers
import io.jacob.episodive.core.designsystem.icon.tabler.Moon
import io.jacob.episodive.core.designsystem.icon.tabler.PhotoExclamation
import io.jacob.episodive.core.designsystem.icon.tabler.PlayerPause
import io.jacob.episodive.core.designsystem.icon.tabler.PlayerPlay
import io.jacob.episodive.core.designsystem.icon.tabler.PlayerSkipBack
import io.jacob.episodive.core.designsystem.icon.tabler.PlayerSkipForward
import io.jacob.episodive.core.designsystem.icon.tabler.PlaylistX
import io.jacob.episodive.core.designsystem.icon.tabler.Plus
import io.jacob.episodive.core.designsystem.icon.tabler.RewindBackward15
import io.jacob.episodive.core.designsystem.icon.tabler.RewindForward30
import io.jacob.episodive.core.designsystem.icon.tabler.Share
import io.jacob.episodive.core.designsystem.icon.tabler.Tabler
import io.jacob.episodive.core.designsystem.icon.tabler.Transfer
import io.jacob.episodive.core.designsystem.icon.tabler.TransitionTop
import io.jacob.episodive.core.designsystem.icon.tabler.UsersMinus
import io.jacob.episodive.core.designsystem.icon.tabler.UsersPlus
import io.jacob.episodive.core.designsystem.icon.tabler.WorldShare
import io.jacob.episodive.core.designsystem.icon.tabler.X
import io.jacob.episodive.core.designsystem.icon.tabler.Zoom
import io.jacob.episodive.core.designsystem.icon.tabler.ZoomFilled

object EpisodiveIcons {
    val Add = Tabler.Plus
    val ArrowBack = Tabler.ArrowLeft
    val CaretDown = Tabler.ChevronDown
    val CaretRight = Tabler.ChevronRight
    val Caption = Tabler.BadgeCc
    val Check = Tabler.Check
    val Clip = Tabler.Container
    val ClipFilled = Tabler.ContainerFilled
    val Close = Tabler.X
    val Collapse = Tabler.ArrowsDiagonalMinimize2
    val Download = Icons.Default.Download
    val DownloadDone = Icons.Default.DownloadDone
    val Error = Tabler.PhotoExclamation
    val Expand = Tabler.ArrowsDiagonal
    val Export = Tabler.FileExport
    val Forward30 = Tabler.RewindForward30
    val History = Tabler.History
    val Home = Tabler.Blob
    val HomeFilled = Tabler.BlobFilled
    val Import = Tabler.FileImport
    val Label = Tabler.Label
    val LetterI = Tabler.LetterISmall
    val LetterX = Tabler.LetterXSmall
    val Library = Tabler.Fountain
    val LibraryFilled = Tabler.FountainFilled
    val Like = Tabler.HeartPlus
    val LikeFilled = Tabler.HeartFilled
    val List = Tabler.List
    val ListNumbered = Tabler.ListNumbers
    val Moon = Tabler.Moon
    val MoreVert = Tabler.DotsVertical
    val Owner = Tabler.CreativeCommonsBy
    val Pause = Tabler.PlayerPause
    val PersonAdd = Tabler.UsersPlus
    val PersonRemove = Tabler.UsersMinus
    val Play = Tabler.PlayerPlay
    val PlaylistX = Tabler.PlaylistX
    val PublicationDate = Tabler.CalendarTime
    val Replay15 = Tabler.RewindBackward15
    val Save = Tabler.DeviceFloppy
    val SaveFilled = Tabler.DeviceFloppyFilled
    val Search = Tabler.Zoom
    val SearchFilled = Tabler.ZoomFilled

    /**
     * 공유. [WorldShare] 와 나누어 쓴다 — 저쪽은 "외부 웹사이트 열기" 전용이라
     * (채널 상세 푸터·팟캐스트 웹사이트) 한 글리프에 두 뜻을 지우면 구분이 사라진다.
     */
    val Share = Tabler.Share
    val SkipNext = Tabler.PlayerSkipForward
    val SkipPrevious = Tabler.PlayerSkipBack

    /**
     * 구독 목록을 파일로 주고받는 자리(OPML). 방향이 하나로 정해지지 않은 진입점이라
     * [Import]·[Export] 어느 쪽도 쓰지 않는다 — 시트를 열면 그 안에서 방향을 고른다.
     */
    val Transfer = Tabler.Transfer
    val TransitionTop = Tabler.TransitionTop
    val WorldShare = Tabler.WorldShare
}
