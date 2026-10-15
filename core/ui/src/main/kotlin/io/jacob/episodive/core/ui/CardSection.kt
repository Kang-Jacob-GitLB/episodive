package io.jacob.episodive.core.ui

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.jacob.episodive.core.designsystem.component.EpisodiveViewToggleHeader

/**
 * 제목 헤더와 펼침 표시가 있는 정보 카드. 카드 전체가 누르는 곳이고 펼침 상태는 호출한 쪽이 쥔다. 높이는
 * [animateContentSize] 로 바뀐다. [outerPadding] 은 카드 바깥 여백이다(목록 안에 놓으면 기본값, 이미 여백이 있는 곳이면 줄인다). 에피소드 플레이어의 에피소드·팟캐스트·챕터 카드와 라디오 편성표가 함께 쓴다.
 */
@Composable
fun CardSection(
    modifier: Modifier = Modifier,
    title: String,
    expanded: Boolean,
    onClick: () -> Unit,
    outerPadding: PaddingValues = PaddingValues(16.dp),
    content: @Composable ColumnScope.() -> Unit,
) {
    Box(
        modifier = modifier
            .padding(outerPadding)
    ) {
        Card(
            modifier = Modifier
                .animateContentSize(),
            onClick = onClick,
            shape = MaterialTheme.shapes.extraLarge,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp, horizontal = 24.dp),
            ) {
                // 버튼이 아니라 표시용 헤더다. 카드 전체가 이미 클릭 대상이라 여기에
                // 버튼을 겹쳐 두면 클릭 지점이 둘로 갈린다 — 제목이나 아이콘을 누르면
                // 리플이 그 언저리에만 번지고, 스크린리더도 같은 동작을 두 번 읽는다.
                EpisodiveViewToggleHeader(
                    modifier = Modifier.padding(bottom = 4.dp),
                    expanded = expanded,
                    contentPadding = PaddingValues(0.dp),
                    text = {
                        Text(
                            modifier = Modifier.weight(1f),
                            text = title,
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        )
                    }
                )

                content()
            }
        }
    }
}
