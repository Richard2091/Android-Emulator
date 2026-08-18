package com.richard.retrohall

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.click
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * 通过真实 UI 流程验证"能玩"：大厅搜索魂斗罗 → 详情页开始 → 核心加载并渲染帧。
 *
 * GameScreen 有 60fps 帧动画，Compose 测试的语义树查询会因永不空闲而失败，
 * 因此本测试只负责驱动 UI 流程并保持游戏运行，核心加载与帧发布由 logcat
 * （RetroHallLibretro / RetroHallFrame）实证，画面渲染由测试运行期间的
 * 外部 adb screencap 实证。
 */
@RunWith(AndroidJUnit4::class)
class GameplayFlowTest {

    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    @Test
    fun startContraGameAndShowFrame() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        copyPrivateAssets(context)

        // 等待大厅出现侧边栏"游戏库"。
        compose.waitUntil(timeoutMillis = 20_000) {
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

        // 等待搜索框并输入游戏名。
        compose.waitUntil(timeoutMillis = 10_000) {
            compose.onAllNodes(hasSetTextAction()).fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNode(hasSetTextAction()).performTextInput("魂斗罗")

        // 点击游戏卡片（tile 的 content-desc 是游戏标题）。
        compose.waitUntil(timeoutMillis = 10_000) {
            compose.onAllNodesWithContentDescription("魂斗罗").fetchSemanticsNodes().isNotEmpty()
        }
        println("[GameplayTest] tile visible, clicking")
        compose.onNodeWithContentDescription("魂斗罗").performTouchInput { click() }
        compose.waitForIdle()
        val detailCount = compose.onAllNodes(hasText("最近游玩")).fetchSemanticsNodes().size
        val startCount = compose.onAllNodes(hasText("开始游戏")).fetchSemanticsNodes().size
        println("[GameplayTest] after click: detail=$detailCount start=$startCount")

        // 详情页特征：等待"最近游玩"文本（进入详情页成功的标志）。
        compose.waitUntil(timeoutMillis = 20_000) {
            compose.onAllNodes(hasText("最近游玩")).fetchSemanticsNodes().isNotEmpty()
        }
        println("[GameplayTest] detail page reached")

        // 详情页点击"开始游戏"。
        compose.waitUntil(timeoutMillis = 20_000) {
            compose.onAllNodes(hasText("开始游戏")).fetchSemanticsNodes().isNotEmpty()
        }
        println("[GameplayTest] start button found")
        compose.onNodeWithText("开始游戏").performClick()

        // 等待核心启动并渲染首帧，然后写标志文件供外部截图。
        Thread.sleep(12_000)
        println("[GameplayTest] core rendered, checking app alive")

        val alive = try {
            InstrumentationRegistry.getInstrumentation().uiAutomation.rootInActiveWindow != null
        } catch (e: Exception) {
            println("[GameplayTest] root fetch error: ${e.message}")
            false
        }
        println("[GameplayTest] app alive after starting game: $alive")

        println("[GameplayTest] writing ready marker")
        try {
            java.io.File(context.filesDir, "game-ready").writeText("done")
            println("[GameplayTest] marker written to ${context.filesDir}/game-ready")
        } catch (e: Exception) {
            println("[GameplayTest] marker write failed: ${e.message}")
        }
        println("[GameplayTest] keeping game running 30s for external screenshot")
        Thread.sleep(30_000)
    }

    private fun copyPrivateAssets(context: android.content.Context) {
        val filesRoot = context.filesDir
        val abi = android.os.Build.SUPPORTED_ABIS.first()
        copyAsset(context, "retrohall_private/cores/$abi/fceumm_libretro_android.so",
            java.io.File(filesRoot, "cores/$abi/fceumm_libretro_android.so"))
        copyAsset(context, "retrohall_private/roms/contra.nes",
            java.io.File(filesRoot, "roms/contra.nes"))
    }

    private fun copyAsset(context: android.content.Context, assetPath: String, target: java.io.File) {
        if (target.isFile) return
        target.parentFile?.mkdirs()
        context.assets.open(assetPath).use { input ->
            target.outputStream().use { output -> input.copyTo(output) }
        }
    }
}
