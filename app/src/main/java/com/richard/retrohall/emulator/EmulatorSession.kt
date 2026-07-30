package com.richard.retrohall.emulator

import com.richard.retrohall.domain.game.LocalGame
import com.richard.retrohall.domain.input.GameAction
import com.richard.retrohall.domain.save.SaveSlot

interface EmulatorSession {
    val state: EmulatorState
    val lastInput: GameAction?

    fun load(game: LocalGame)
    fun start()
    fun pause()
    fun resume()
    fun reset()
    fun stop()
    fun sendInput(action: GameAction, pressed: Boolean)
    fun saveSram(): Boolean
    fun saveState(slot: SaveSlot): Boolean
    fun loadState(slot: SaveSlot): Boolean
}
