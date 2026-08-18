package com.richard.retrohall.emulator

import com.richard.retrohall.domain.game.LocalGame
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class LibretroEmulatorSessionTest {
    @get:Rule
    val temp = TemporaryFolder()

    @Test
    fun movesToErrorWhenFirstFrameFails() {
        val host = FakeLibretroCoreHost(runFrameResult = false)
        val session = LibretroEmulatorSession("core.so", temp.root.absolutePath) { host }

        session.load(sampleGame())
        session.start()

        assertEquals(EmulatorState.Error, session.state)
    }

    @Test
    fun stopUnloadsCoreAfterRunning() {
        val host = FakeLibretroCoreHost(runFrameResult = true)
        val session = LibretroEmulatorSession("core.so", temp.root.absolutePath) { host }

        session.load(sampleGame())
        session.start()
        session.stop()

        assertEquals(EmulatorState.Stopped, session.state)
        assertTrue(host.unloadCoreCalled)
    }

    private fun sampleGame(): LocalGame = LocalGame(
        id = "sample",
        title = "Sample",
        platform = "FC/NES",
        category = "Test",
        coverPath = "",
        romPath = temp.newFile("sample.nes").absolutePath,
    )
}

private class FakeLibretroCoreHost(
    private val loadCoreResult: Boolean = true,
    private val loadGameResult: Boolean = true,
    private val runFrameResult: Boolean = true,
) : LibretroCoreHost {
    var unloadCoreCalled: Boolean = false

    override fun nativeVersion(): String = "test"
    override fun loadCore(corePath: String): Boolean = loadCoreResult
    override fun unloadCore() {
        unloadCoreCalled = true
    }
    override fun loadGame(romPath: String): Boolean = loadGameResult
    override fun runFrame(): Boolean = runFrameResult
    override fun reset() = Unit
    override fun serializeState(path: String): Boolean = true
    override fun unserializeState(path: String): Boolean = true
    override fun saveSram(path: String): Boolean = true
    override fun setInputState(actionName: String, pressed: Boolean) = Unit
    override fun getFrameInfo(): NativeFrameInfo? = NativeFrameInfo(width = 0, height = 0, ready = false)
    override fun pollFrame(dst: ByteArray): Int = 0
    override fun getAvInfo(): NativeAvInfo? = NativeAvInfo(fps = 60.0, sampleRate = 44100, channels = 2, baseWidth = 256, baseHeight = 224)
    override fun drainAudio(dst: ByteArray): Int = 0
    override fun resetAudio() = Unit
}
