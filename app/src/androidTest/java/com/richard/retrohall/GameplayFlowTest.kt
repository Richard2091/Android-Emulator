package com.richard.retrohall

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.click
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import android.os.ParcelFileDescriptor
import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * 通过真实 UI 流程验证"能玩"：数据源同步 → 搜索游戏 → 详情页开始 → 核心加载并渲染帧。
 *
 * 只消费数据源资源：游戏目录从在线 index.v2 同步，核心由核心管理按需从在线仓库下载。
 * 本测试会真实下载 ROM 与核心，需要网络。
 */
@RunWith(AndroidJUnit4::class)
class GameplayFlowTest {

    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    @Test
    fun startDataSourceGameAndShowFrame() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        clearLogcat()

        // 等待大厅出现侧边栏"游戏库"（数据源同步完成后才有内容）。
        compose.waitUntil(timeoutMillis = 30_000) {
            compose.onAllNodes(hasText("游戏库")).fetchSemanticsNodes().isNotEmpty()
        }

        // 下拉手势显示工具栏（搜索框随之出现）。
        compose.onNode(isRoot()).performTouchInput {
            swipe(
                start = Offset(width / 2f, height * 0.08f),
                end = Offset(width / 2f, height * 0.55f),
                durationMillis = 300,
            )
        }

        // 等待搜索框并输入游戏名（在线目录首条游戏，英文名便于索引命中）。
        compose.waitUntil(timeoutMillis = 10_000) {
            compose.onAllNodes(hasSetTextAction()).fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNode(hasSetTextAction()).performTextInput("Donkey Kong")

        // 点击游戏卡片（tile 的 content-desc 是游戏标题）。
        compose.waitUntil(timeoutMillis = 20_000) {
            compose.onAllNodesWithContentDescription("Donkey Kong").fetchSemanticsNodes().isNotEmpty()
        }
        println("[GameplayTest] tile visible, clicking")
        compose.onAllNodesWithContentDescription("Donkey Kong")[0].performTouchInput { click() }
        compose.waitForIdle()
        val detailCount = compose.onAllNodes(hasText("最近游玩")).fetchSemanticsNodes().size
        val startCount = compose.onAllNodes(hasText("开始游戏")).fetchSemanticsNodes().size
        println("[GameplayTest] after click: detail=$detailCount start=$startCount")

        // 详情页特征：等待"最近游玩"文本（进入详情页成功的标志）。
        compose.waitUntil(timeoutMillis = 20_000) {
            compose.onAllNodes(hasText("最近游玩")).fetchSemanticsNodes().isNotEmpty()
        }
        println("[GameplayTest] detail page reached")

        // 详情页点击"开始游戏"（首次会触发 ROM 下载；核心由核心管理自动下载并选中）。
        compose.waitUntil(timeoutMillis = 20_000) {
            compose.onAllNodes(hasText("开始游戏")).fetchSemanticsNodes().isNotEmpty()
        }
        println("[GameplayTest] start button found")
        compose.onNodeWithText("开始游戏").performTouchInput { click() }

        // 等待应用日志确认游戏页已切换并开始出帧。
        waitForLogcatMessages(
            "RetroHallApp: onStart ok",
            "RetroHallApp: route switched to Game",
            "RetroHallFrame: frame published",
            timeoutMs = 60_000,
        )

        val alive = InstrumentationRegistry.getInstrumentation().uiAutomation.rootInActiveWindow != null
        println("[GameplayTest] app alive after starting game: $alive")
        assertTrue(alive)
    }

    private fun clearLogcat() {
        InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand("logcat -c").close()
    }

    private fun waitForLogcatMessages(vararg needles: String, timeoutMs: Long) {
        val deadline = System.currentTimeMillis() + timeoutMs
        val automation = InstrumentationRegistry.getInstrumentation().uiAutomation
        while (System.currentTimeMillis() < deadline) {
            try {
                val logs = readLogcat(automation)
                if (needles.all { logs.contains(it) }) {
                    return
                }
            } catch (_: Exception) {
                // 忽略瞬时不可用状态，继续轮询。
            }
            Thread.sleep(200)
        }
        error("Timeout waiting for logcat=${needles.joinToString()}")
    }

    private fun readLogcat(automation: android.app.UiAutomation): String {
        automation.executeShellCommand("logcat -d -s RetroHallApp:I RetroHallLibretro:I RetroHallFrame:I").use { pfd ->
            ParcelFileDescriptor.AutoCloseInputStream(pfd).use { input ->
                return BufferedReader(InputStreamReader(input)).readText()
            }
        }
    }
}
