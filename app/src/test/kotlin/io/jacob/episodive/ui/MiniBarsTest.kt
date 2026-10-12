package io.jacob.episodive.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * 스테이션이 켜지는 프레임에 에피소드 미니바의 `suppressed` 가 같은 구성에서 참이 되는지 고정한다.
 * 둘을 각자 판정하면 한 프레임 겹쳐 보인다.
 */
@RunWith(RobolectricTestRunner::class)
class MiniBarsTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun stationActive_suppressesPlayerBarInTheSameComposition() {
        var isStationActive by mutableStateOf(false)
        val seen = mutableListOf<Boolean>()

        composeTestRule.setContent {
            Box {
                MiniBars(
                    isStationActive = isStationActive,
                    playerBar = { suppressed ->
                        seen += suppressed
                        Text("player")
                    },
                    stationBar = { Text("station") },
                )
            }
        }
        composeTestRule.waitForIdle()
        assertEquals(false, seen.last())

        isStationActive = true
        composeTestRule.waitForIdle()

        assertEquals(true, seen.last())
        // 에피소드 바는 계속 컴포즈된다(복원·저장 수집기가 그 ViewModel 에 있다).
        composeTestRule.onNodeWithText("player").assertExists()
        composeTestRule.onNodeWithText("station").assertExists()
    }
}
