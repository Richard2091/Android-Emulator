package com.richard.retrohall.emulator

import com.richard.retrohall.domain.game.LocalGame
import com.richard.retrohall.domain.input.GameAction
import com.richard.retrohall.domain.save.SavePathResolver
import com.richard.retrohall.domain.save.SaveSlot
import java.io.File

class LibretroEmulatorSession(
    private val corePath: String,
    private val filesRoot: String,
    private val hostProvider: () -> LibretroCoreHost = { LibretroHost() },
) : EmulatorSession {
    private val hostLock = Any()
    private var host: LibretroCoreHost? = null
    private var loadedGame: LocalGame? = null
    private var frameThread: Thread? = null

    @Volatile
    override var state: EmulatorState = EmulatorState.Idle
        private set

    override var lastInput: GameAction? = null
        private set

    override fun load(game: LocalGame) {
        state = EmulatorState.Idle
        stopFrameLoop()
        synchronized(hostLock) {
            runCatching { host?.unloadCore() }
        }
        host = null
        loadedGame = game
        val nextHost = runCatching { hostProvider() }.getOrNull()
        val loaded = nextHost != null && synchronized(hostLock) {
            nextHost.loadCore(corePath) && nextHost.loadGame(game.romPath)
        }
        if (!loaded) {
            synchronized(hostLock) {
                runCatching { nextHost?.unloadCore() }
            }
            host = null
            state = EmulatorState.Error
            return
        }
        host = nextHost
        state = EmulatorState.Loaded
    }

    override fun start() {
        if (state != EmulatorState.Loaded && state != EmulatorState.Paused) {
            state = EmulatorState.Error
            return
        }
        val firstFrameOk = synchronized(hostLock) {
            runCatching { host?.runFrame() == true }.getOrDefault(false)
        }
        if (!firstFrameOk) {
            state = EmulatorState.Error
            return
        }
        state = EmulatorState.Running
        startFrameLoop()
    }

    override fun pause() {
        if (state == EmulatorState.Running) state = EmulatorState.Paused
    }

    override fun resume() {
        if (state == EmulatorState.Paused) {
            state = EmulatorState.Running
            startFrameLoop()
        }
    }

    override fun reset() {
        val canReset = synchronized(hostLock) {
            host?.reset()
            host != null
        }
        if (canReset && loadedGame != null) state = EmulatorState.Running
    }

    override fun stop() {
        state = EmulatorState.Stopped
        stopFrameLoop()
        synchronized(hostLock) {
            runCatching { host?.unloadCore() }
        }
        host = null
    }

    override fun sendInput(action: GameAction, pressed: Boolean) {
        if (pressed) lastInput = action
        synchronized(hostLock) {
            host?.setInputState(action.toLibretroActionName(), pressed)
        }
    }

    override fun saveSram(): Boolean {
        val game = loadedGame ?: return false
        return writeWithParentDirs(SavePathResolver.sramPath(filesRoot, game.id)) { path ->
            synchronized(hostLock) { host?.saveSram(path) == true }
        }
    }

    override fun saveState(slot: SaveSlot): Boolean {
        val game = loadedGame ?: return false
        return writeWithParentDirs(SavePathResolver.statePath(filesRoot, game.id, slot)) { path ->
            synchronized(hostLock) { host?.serializeState(path) == true }
        }
    }

    override fun loadState(slot: SaveSlot): Boolean {
        val game = loadedGame ?: return false
        val path = SavePathResolver.statePath(filesRoot, game.id, slot)
        return File(path).isFile && synchronized(hostLock) { host?.unserializeState(path) == true }
    }

    private fun startFrameLoop() {
        if (frameThread?.isAlive == true) return
        frameThread = Thread {
            while (state == EmulatorState.Running || state == EmulatorState.Paused) {
                if (state == EmulatorState.Paused) {
                    runCatching { Thread.sleep(16) }
                    continue
                }
                val ok = synchronized(hostLock) {
                    runCatching { host?.runFrame() == true }.getOrDefault(false)
                }
                if (!ok) {
                    state = EmulatorState.Error
                    break
                }
                runCatching { Thread.sleep(16) }
            }
        }.apply {
            name = "RetroHall-LibretroFrameLoop"
            isDaemon = true
            start()
        }
    }

    private fun stopFrameLoop() {
        val thread = frameThread
        frameThread = null
        if (thread != null && thread !== Thread.currentThread()) {
            thread.interrupt()
            runCatching { thread.join(200) }
        }
    }

    private fun writeWithParentDirs(path: String, writer: (String) -> Boolean): Boolean {
        File(path).parentFile?.mkdirs()
        return writer(path)
    }

    private fun GameAction.toLibretroActionName(): String = when (this) {
        GameAction.Up -> "Up"
        GameAction.Down -> "Down"
        GameAction.Left -> "Left"
        GameAction.Right -> "Right"
        GameAction.Confirm,
        GameAction.NesA -> "A"
        GameAction.Back,
        GameAction.NesB -> "B"
        GameAction.Menu,
        GameAction.Start -> "Start"
        GameAction.Select -> "Select"
    }
}
