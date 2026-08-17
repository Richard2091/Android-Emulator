package com.richard.retrohall.emulator

import androidx.test.core.app.ApplicationProvider
import com.richard.retrohall.domain.game.LocalGame
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class EmulatorSessionFactoryTest {
    @Test
    fun fallsBackToFakeSessionWhenCoreIsMissing() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        context.filesDir.deleteRecursively()
        context.filesDir.mkdirs()
        val game = LocalGame(
            id = "sample",
            title = "Sample",
            platform = "FC/NES",
            category = "Test",
            coverPath = "",
            romPath = "files/roms/sample.nes",
        )

        val result = EmulatorSessionFactory(context).createStartedSession(game)

        assertTrue(result.session is FakeEmulatorSession)
        assertEquals(EmulatorState.Running, result.session.state)
        assertTrue(result.message?.contains("兼容演示模式") == true)
    }
}
