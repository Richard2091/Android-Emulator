package com.richard.retrohall

import android.app.UiAutomation
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.ParcelFileDescriptor
import android.util.Log
import android.view.accessibility.AccessibilityNodeInfo
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.click
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.richard.retrohall.data.settings.UserSettingsStore
import com.richard.retrohall.domain.settings.ControlMode
import com.richard.retrohall.domain.settings.UserSettings
import com.richard.retrohall.domain.settings.VirtualPadVisibility
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertNotNull
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * 验证游玩界面虚拟按键行为：
 *  1. 常驻模式：摇杆在左半屏、ABXY 在右半屏、底部按钮在屏幕下半部（分别从对应方向弹出）。
 *  2. 自动隐藏模式：隐藏后点击屏幕能重新唤出。
 *
 * GameScreen 有 60fps 帧动画，Compose 语义树查询会因永不空闲而失败，
 * 因此进入游戏画面后一律改用 UiAutomation 访问无障碍树做断言。
 */
@RunWith(AndroidJUnit4::class)
class VirtualPadFlowTest {

    @get:Rule
    val compose = createEmptyComposeRule()

    private val context: Context
        get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Before
    fun setUp() {
        copyPrivateAssets(context)
    }

    private var scenario: ActivityScenario<MainActivity>? = null

    @After
    fun tearDown() {
        scenario?.close()
        scenario = null
    }

    @Test
    fun visibleMode_padLayoutPositions() {
        writeSettings(VirtualPadVisibility.Visible)
        scenario = ActivityScenario.launch(MainActivity::class.java)
        startContra()
        waitForLogcatMessages("RetroHallFrame: frame published", timeoutMs = 30_000)
        compose.mainClock.advanceTimeBy(1500)
        Thread.sleep(2000)

        val bmp = captureScreenshot()
        val w = bmp.width
        val h = bmp.height
        val joyCyan = countCyan(bmp, 0, w / 2, 0, h)
        val abxyCyan = countCyan(bmp, w / 2, w, 0, h)
        Log.e("VirtualPadTest", "size=${w}x$h joyCyan=$joyCyan abxyCyan=$abxyCyan")
        assertTrue("常驻模式下摇杆应显示(左半屏青色描边, got=$joyCyan)", joyCyan > 300)
        assertTrue("常驻模式下ABXY应显示(右半屏青色A键, got=$abxyCyan)", abxyCyan > 800)

        val bottom = (h * 0.78f).toInt()
        val leftDark = countDark(bmp, 0, w / 2, bottom, h)
        val rightDark = countDark(bmp, w / 2, w, bottom, h)
        val leftWhite = countWhite(bmp, 0, w / 2, bottom, h)
        val rightWhite = countWhite(bmp, w / 2, w, bottom, h)
        Log.e(
            "VirtualPadTest",
            "bottom=$bottom leftDark=$leftDark rightDark=$rightDark leftWhite=$leftWhite rightWhite=$rightWhite",
        )
        assertTrue("左侧底部按钮(设置/暂停)应显示", leftDark > 3000 && leftWhite > 50)
        assertTrue("右侧底部按钮(选择/开始)应显示", rightDark > 3000 && rightWhite > 50)
    }

    @Test
    fun autoHideMode_hiddenThenTapRevealsPad() {
        writeSettings(VirtualPadVisibility.AutoHide)
        scenario = ActivityScenario.launch(MainActivity::class.java)
        startContra()
        waitForLogcatMessages("RetroHallFrame: frame published", timeoutMs = 30_000)
        compose.mainClock.advanceTimeBy(1500)
        Thread.sleep(2000)

        val pad0 = countPadPixels()
        Log.e("VirtualPadTest", "initial pad=$pad0")
        dumpEdgeSample("initial")
        assertTrue("自动隐藏模式初始应隐藏按键 (pad=$pad0)", pad0 < 100)

        tapCenter()
        waitForPadAbove(800, 5_000)
        assertTrue("点击屏幕后应唤出虚拟按键 (pad=$lastCyan)", lastCyan > 800)

        waitForPadBelow(100, 8_000)
        assertTrue("无交互后应自动隐藏 (pad=$lastCyan)", lastCyan < 100)

        tapCenter()
        waitForPadAbove(800, 5_000)
        assertTrue("隐藏后再次点击应能唤出 (pad=$lastCyan)", lastCyan > 800)
    }

    private fun writeSettings(visibility: VirtualPadVisibility) {
        runBlocking {
            val store = UserSettingsStore(context)
            store.update(
                UserSettings(
                    controlMode = ControlMode.VirtualPad,
                    virtualPadVisibility = visibility,
                )
            )
            val readBack = store.settings.first()
            Log.e(
                "VirtualPadTest",
                "writeSettings=$visibility readBack=${readBack.virtualPadVisibility} mode=${readBack.controlMode}",
            )
            check(readBack.virtualPadVisibility == visibility) {
                "设置写入未生效: want=$visibility got=${readBack.virtualPadVisibility}"
            }
        }
    }

    private fun startContra() {
        clearLogcat()
        compose.waitUntil(timeoutMillis = 20_000) {
            compose.onAllNodes(hasText("游戏库")).fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNode(isRoot()).performTouchInput {
            swipe(
                start = Offset(width / 2f, height * 0.08f),
                end = Offset(width / 2f, height * 0.55f),
                durationMillis = 300,
            )
        }
        compose.waitUntil(timeoutMillis = 10_000) {
            compose.onAllNodes(hasSetTextAction()).fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNode(hasSetTextAction()).performTextInput("魂斗罗")
        compose.waitUntil(timeoutMillis = 10_000) {
            compose.onAllNodesWithContentDescription("魂斗罗").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onAllNodesWithContentDescription("魂斗罗")[0].performTouchInput { click() }
        compose.waitUntil(timeoutMillis = 20_000) {
            compose.onAllNodes(hasText("最近游玩")).fetchSemanticsNodes().isNotEmpty()
        }
        compose.waitUntil(timeoutMillis = 20_000) {
            compose.onAllNodes(hasText("开始游戏")).fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText("开始游戏").performTouchInput { click() }
        waitForLogcatMessages("RetroHallApp: route switched to Game", timeoutMs = 30_000)
    }

    private fun tapCenter() {
        val dm = context.resources.displayMetrics
        val x = dm.widthPixels / 2
        val y = dm.heightPixels / 2
        InstrumentationRegistry.getInstrumentation().uiAutomation
            .executeShellCommand("input tap $x $y").close()
    }

    private var lastCyan = 0

    private fun countPadPixels(): Int {
        val bmp = captureScreenshot()
        val w = bmp.width
        val h = bmp.height
        return countCyan(bmp, 0, (w * 0.3f).toInt(), 0, h) +
            countGold(bmp, (w * 0.7f).toInt(), w, 0, h)
    }

    private fun waitForPadAbove(threshold: Int, timeoutMs: Long) {
        waitForCondition(timeoutMs) {
            compose.mainClock.advanceTimeBy(100)
            lastCyan = countPadPixels()
            lastCyan > threshold
        }
    }

    private fun waitForPadBelow(threshold: Int, timeoutMs: Long) {
        waitForCondition(timeoutMs) {
            Thread.sleep(300)
            compose.mainClock.advanceTimeBy(600)
            lastCyan = countPadPixels()
            lastCyan < threshold
        }
    }

    private fun captureScreenshot(): Bitmap {
        val pfd = InstrumentationRegistry.getInstrumentation().uiAutomation
            .executeShellCommand("screencap -p")
        val bytes = ParcelFileDescriptor.AutoCloseInputStream(pfd).use { it.readBytes() }
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)!!
    }

    private fun countGold(bmp: Bitmap, x0: Int, x1: Int, y0: Int, y1: Int): Int {
        var n = 0
        for (y in y0 until y1) {
            for (x in x0 until x1) {
                val c = bmp.getPixel(x, y)
                val r = (c ushr 16) and 0xFF
                val g = (c ushr 8) and 0xFF
                val b = c and 0xFF
                if (r > 130 && g > 110 && b < 160 && r - b > 40) n++
            }
        }
        return n
    }

    private fun dumpEdgeSample(tag: String) {
        val bmp = captureScreenshot()
        val w = bmp.width
        val h = bmp.height
        val x0 = (w * 0.7f).toInt()
        val x1 = w
        var nonBlack = 0
        for (y in 0 until h step 2) {
            for (x in x0 until x1 step 2) {
                val c = bmp.getPixel(x, y)
                val r = (c ushr 16) and 0xFF
                val g = (c ushr 8) and 0xFF
                val b = c and 0xFF
                if (r > 30 || g > 30 || b > 30) nonBlack++
            }
        }
        val samples = buildString {
            for (py in listOf(h / 4, h / 2, 3 * h / 4)) {
                for (px in listOf(x0, x0 + (x1 - x0) / 3, x0 + 2 * (x1 - x0) / 3, x1 - 1)) {
                    val c = bmp.getPixel(px, py)
                    append("($px,$py)=#")
                    append(String.format("%06X", c and 0xFFFFFF))
                    append("  ")
                }
                append("\n")
            }
        }
        Log.e("VirtualPadTest", "$tag rightNonBlack=$nonBlack samples:\n$samples")
    }

    private fun countCyan(bmp: Bitmap, x0: Int, x1: Int, y0: Int, y1: Int): Int {
        var n = 0
        for (y in y0 until y1) {
            for (x in x0 until x1) {
                val c = bmp.getPixel(x, y)
                val r = (c ushr 16) and 0xFF
                val g = (c ushr 8) and 0xFF
                val b = c and 0xFF
                if (g - r > 40 && g > 80) n++
            }
        }
        return n
    }

    private fun countDark(bmp: Bitmap, x0: Int, x1: Int, y0: Int, y1: Int): Int {
        var n = 0
        for (y in y0 until y1) {
            for (x in x0 until x1) {
                val c = bmp.getPixel(x, y)
                val r = (c ushr 16) and 0xFF
                val g = (c ushr 8) and 0xFF
                val b = c and 0xFF
                if (r < 60 && g < 60 && b < 60) n++
            }
        }
        return n
    }

    private fun countWhite(bmp: Bitmap, x0: Int, x1: Int, y0: Int, y1: Int): Int {
        var n = 0
        for (y in y0 until y1) {
            for (x in x0 until x1) {
                val c = bmp.getPixel(x, y)
                val r = (c ushr 16) and 0xFF
                val g = (c ushr 8) and 0xFF
                val b = c and 0xFF
                if (r > 150 && g > 150 && b > 150) n++
            }
        }
        return n
    }

    private fun waitForCondition(timeoutMs: Long, condition: () -> Boolean) {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            if (condition()) return
            Thread.sleep(120)
        }
        error("Timeout waiting for condition")
    }

    private fun copyPrivateAssets(context: Context) {
        val filesRoot = context.filesDir
        val abi = android.os.Build.SUPPORTED_ABIS.first()
        copyAsset(
            context,
            "retrohall_private/cores/$abi/fceumm_libretro_android.so",
            java.io.File(filesRoot, "cores/$abi/fceumm_libretro_android.so"),
        )
        copyAsset(
            context,
            "retrohall_private/roms/contra.nes",
            java.io.File(filesRoot, "roms/contra.nes"),
        )
    }

    private fun copyAsset(context: Context, assetPath: String, target: java.io.File) {
        if (target.isFile) return
        target.parentFile?.mkdirs()
        context.assets.open(assetPath).use { input ->
            target.outputStream().use { output -> input.copyTo(output) }
        }
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
            }
            Thread.sleep(200)
        }
        error("Timeout waiting for logcat=${needles.joinToString()}")
    }

    private fun readLogcat(automation: UiAutomation): String {
        automation.executeShellCommand("logcat -d -s RetroHallApp:I RetroHallLibretro:I RetroHallFrame:I")
            .use { pfd ->
                ParcelFileDescriptor.AutoCloseInputStream(pfd).use { input ->
                    return BufferedReader(InputStreamReader(input)).readText()
                }
            }
    }

    private fun clearLogcat() {
        InstrumentationRegistry.getInstrumentation().uiAutomation
            .executeShellCommand("logcat -c").close()
    }
}