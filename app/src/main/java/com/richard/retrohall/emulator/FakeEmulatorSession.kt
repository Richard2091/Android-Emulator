package com.richard.retrohall.emulator

import android.graphics.Bitmap
import com.richard.retrohall.domain.game.LocalGame
import com.richard.retrohall.domain.input.GameAction
import com.richard.retrohall.domain.save.SaveSlot
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class FakeEmulatorSession : EmulatorSession {
    private var loadedGame: LocalGame? = null

    override var state: EmulatorState = EmulatorState.Idle
        private set

    override var lastInput: GameAction? = null
        private set

    override val frames: StateFlow<Bitmap?> = MutableStateFlow(null)

    override fun load(game: LocalGame) {
        loadedGame = game
        state = EmulatorState.Loaded
    }

    override fun start() {
        state = if (loadedGame == null) EmulatorState.Error else EmulatorState.Running
    }

    override fun pause() {
        if (state == EmulatorState.Running) state = EmulatorState.Paused
    }

    override fun resume() {
        if (state == EmulatorState.Paused) state = EmulatorState.Running
    }

    override fun reset() {
        if (loadedGame != null) {
            lastInput = null
            state = EmulatorState.Running
        }
    }

    override fun stop() {
        state = EmulatorState.Stopped
    }

    override fun sendInput(action: GameAction, pressed: Boolean) {
        if (pressed) lastInput = action
    }

    override fun saveSram(): Boolean = loadedGame != null

    override fun saveState(slot: SaveSlot): Boolean = loadedGame != null

    override fun loadState(slot: SaveSlot): Boolean = loadedGame != null
}
