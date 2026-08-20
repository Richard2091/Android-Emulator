package com.richard.retrohall

import android.content.Context
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.richard.retrohall.data.core.CoreCatalogClient
import com.richard.retrohall.data.core.CoreDownloadManager
import com.richard.retrohall.data.core.CoreSelectionStore
import com.richard.retrohall.data.game.ContentDownloadManager
import com.richard.retrohall.data.game.ResourceCatalogClient
import com.richard.retrohall.data.settings.ResourceSourceStore
import com.richard.retrohall.domain.game.LocalGame
import com.richard.retrohall.emulator.EmulatorSessionFactory
import com.richard.retrohall.emulator.EmulatorState
import com.richard.retrohall.emulator.LibretroHost
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * 端到端验证：真实资源仓库（在线）+ 模拟器上的下载/使用/删除闭环。
 *
 * 游戏资源：catalog/index.v2.json -> 列表 -> 详情 -> 下载 ROM(sha256) -> 核心加载运行 -> 删除。
 * 核心资源：core-manifest.v1.json -> 下载 .so(sha256) -> dlopen 加载 -> 删除。
 *
 * 注意：x86_64 模拟器无法 dlopen arm64 so（native bridge 不翻译应用自加载库），
 * 因此"使用"环节注入与本机 ABI 匹配的核心（x86_64），与真实设备行为一致。
 */
@RunWith(AndroidJUnit4::class)
class ResourceLifecycleVerificationTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val sourceStore = ResourceSourceStore(context)
    private val resourceCatalogClient = ResourceCatalogClient(sourceStore)
    private val contentDownloadManager = ContentDownloadManager(context)
    private val coreCatalogClient = CoreCatalogClient(sourceStore)
    private val coreDownloadManager = CoreDownloadManager(context)
    private val coreSelectionStore = CoreSelectionStore(context)
    private val emulatorSessionFactory = EmulatorSessionFactory(context, coreCatalogClient, coreSelectionStore)

    @Test
    fun gameResource_download_use_delete() = runBlocking {
        // 0. 注入本机 ABI 核心（模拟真机已安装/已下载核心），供使用环节加载
        ensureDeviceCoreAvailable()

        // 1. 拉取在线目录
        val index = resourceCatalogClient.fetchIndex()
        assertTrue("index 无分类", index.categories.isNotEmpty())
        val fc = index.categories.first { it.id == "fc" }
        val items = resourceCatalogClient.fetchCategoryList(fc.listUrl)
        assertTrue("FC 列表为空", items.isNotEmpty())
        val item = items.first()
        val detail = resourceCatalogClient.fetchDetail(item.detailUrl)
        assertTrue("详情无可用文件", detail.downloadableFiles().isNotEmpty())
        println("[ResourceLifecycle] 游戏 ${item.id} 可下载文件=${detail.downloadableFiles().size}")

        // 2. 下载（含 sha256 校验）
        val game = LocalGame(
            id = item.id,
            title = item.displayTitle(),
            platform = item.primaryPlatformId,
            category = item.categoryId,
            categoryId = item.categoryId,
            platformId = item.primaryPlatformId,
            runtimeFamily = item.runtimeFamily,
            detailUrl = item.detailUrl,
            coverPath = item.coverUrl,
            romPath = "",
        )
        contentDownloadManager.deleteLocal(game.id)
        assertFalse("下载前不应是已下载状态", contentDownloadManager.isDownloaded(game.id))
        val prepared = contentDownloadManager.prepare(game, detail.files)
        assertTrue("下载后应为已下载状态", contentDownloadManager.isDownloaded(prepared.id))
        assertTrue("主文件应已落盘: ${prepared.romPath}", File(prepared.romPath).isFile)
        assertEquals("ROM 文件大小应匹配", detail.primaryFile()?.size ?: -1L, File(prepared.romPath).length())
        println("[ResourceLifecycle] 下载完成 rom=${prepared.romPath} size=${File(prepared.romPath).length()}")

        // 3. 使用：真实核心加载下载的 ROM 并出帧
        val launch = emulatorSessionFactory.createStartedSession(prepared)
        println("[ResourceLifecycle] 会话 state=${launch.session.state} msg=${launch.message}")
        assertEquals("核心会话应处于运行态", EmulatorState.Running, launch.session.state)
        assertTrue("应使用真实核心而非演示模式", launch.message == null)
        var frameSeen = false
        repeat(400) {
            val frame = launch.session.frames.value
            if (frame != null && frame.width > 0 && frame.height > 0) {
                frameSeen = true
                println("[ResourceLifecycle] 收到帧 ${frame.width}x${frame.height}")
                return@repeat
            }
            kotlinx.coroutines.delay(50)
        }
        launch.session.stop()
        assertTrue("下载的 ROM 应能通过核心产出画面帧", frameSeen)
        println("[ResourceLifecycle] 游戏资源 使用验证通过")

        // 4. 删除
        contentDownloadManager.deleteLocal(prepared.id)
        assertFalse("删除后不应再是已下载状态", contentDownloadManager.isDownloaded(prepared.id))
        assertFalse("删除后主文件应不存在", File(prepared.romPath).exists())
        println("[ResourceLifecycle] 游戏资源 删除验证通过")
    }

    @Test
    fun coreResource_download_use_delete() = runBlocking {
        // 1. 拉取在线核心清单
        assertMultiArchManifest()
        val catalog = coreCatalogClient.fetchCatalog()
        assertTrue("核心清单为空", catalog.cores.isNotEmpty())
        val nesCores = catalog.forPlatform("nes")
        assertTrue("NES 无核心", nesCores.isNotEmpty())
        val core = nesCores.first { it.defaultForPlatform } ?: nesCores.first()
        println("[ResourceLifecycle] 核心 ${core.id}(${core.displayName}) abis=${core.files.map { it.abi }}")

        // ABI 匹配：自动选择「设备支持 ABI ∩ 清单 ABI」（x86_64 设备应匹配到 x86_64 核心）。
        val abi = coreDownloadManager.matchingAbi(core)
        assertNotNull("在线核心仓库应包含本设备支持的 ABI 核心: ${coreDownloadManager.supportedAbis}", abi)
        println("[ResourceLifecycle] 下载 ABI=$abi device=${Build.SUPPORTED_ABIS.joinToString()}")

        // 2. 下载（含 sha256 校验）
        coreDownloadManager.delete(core)
        assertFalse("下载前不应是已下载状态", coreDownloadManager.isDownloaded(core))
        coreDownloadManager.download(core)
        assertTrue("下载后应为已下载状态", coreDownloadManager.isDownloaded(core))
        val so = coreDownloadManager.localFile(core.fileFor(abi!!)!!)
        assertTrue("核心 .so 应已落盘: ${so.absolutePath}", so.isFile)
        assertEquals("核心 .so 大小应匹配", core.fileFor(abi)!!.size, so.length())
        println("[ResourceLifecycle] 核心下载完成 size=${so.length()}")

        // 3. 使用：dlopen 加载下载的核心（自动匹配的 ABI 应可直接加载）
        val host = LibretroHost()
        assertTrue("loadCore 应成功: $so", host.loadCore(so.absolutePath))
        val av = host.getAvInfo()
        println("[ResourceLifecycle] loadCore 成功 avInfo=$av")
        host.unloadCore()
        println("[ResourceLifecycle] 核心资源 使用验证通过")

        // 4. 删除（删除的是在线下载的核心）
        coreDownloadManager.delete(core)
        assertFalse("删除后不应是已下载状态", coreDownloadManager.isDownloaded(core))
        assertFalse("删除后 .so 应不存在", so.exists())
        println("[ResourceLifecycle] 核心资源 删除验证通过")
    }

    /** 注入本机 ABI 的 fceumm 核心到 files/cores/<abi>/，返回核心文件。 */
    private fun ensureDeviceCoreAvailable(): File {
        val abi = Build.SUPPORTED_ABIS.first()
        val assetName = when (abi) {
            "x86_64" -> "retrohall_private/cores/x86_64/fceumm_libretro_android.so"
            "x86" -> "retrohall_private/cores/x86/fceumm_libretro_android.so"
            "arm64-v8a" -> "retrohall_private/cores/arm64-v8a/fceumm_libretro_android.so"
            "armeabi-v7a" -> "retrohall_private/cores/armeabi-v7a/fceumm_libretro_android.so"
            else -> "retrohall_private/cores/$abi/fceumm_libretro_android.so"
        }
        val target = File(context.filesDir, "cores/$abi/fceumm_libretro_android.so")
        if (!target.isFile) {
            target.parentFile?.mkdirs()
            context.assets.open(assetName).use { input ->
                target.outputStream().use { output -> input.copyTo(output) }
            }
            println("[ResourceLifecycle] 注入核心 $assetName -> $target (${target.length()}B)")
        }
        return target
    }

    /** 断言在线核心清单包含 x86/x86_64（多架构发布）。 */
    private suspend fun assertMultiArchManifest() = runBlocking {
        val catalog = coreCatalogClient.fetchCatalog()
        val fceumm = catalog.forPlatform("nes").first { it.id == "fceumm" }
        val abis = fceumm.files.map { it.abi }
        assertTrue("清单应包含 x86: $abis", "x86" in abis)
        assertTrue("清单应包含 x86_64: $abis", "x86_64" in abis)
        println("[ResourceLifecycle] 在线清单 ABI=$abis")
    }
}
