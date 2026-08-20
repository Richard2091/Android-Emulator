package com.richard.retrohall.emulator

import androidx.test.core.app.ApplicationProvider
import com.richard.retrohall.data.core.CoreCatalogClient
import com.richard.retrohall.data.core.CoreSelectionStore
import com.richard.retrohall.data.settings.ResourceSourceStore
import com.richard.retrohall.domain.game.LocalGame
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class EmulatorSessionFactoryTest {
    @Test
    fun fallsBackToFakeSessionWhenCoreIsMissing() = runBlocking {
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

        val factory = EmulatorSessionFactory(
            context,
            CoreCatalogClient(ResourceSourceStore(context)),
            CoreSelectionStore(context),
        )
        val result = factory.createStartedSession(game)

        assertTrue(result.session is FakeEmulatorSession)
        assertEquals(EmulatorState.Running, result.session.state)
        assertTrue(result.message?.contains("兼容演示模式") == true)
    }
}
