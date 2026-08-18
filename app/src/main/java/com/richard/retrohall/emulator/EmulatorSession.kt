package com.richard.retrohall.emulator

import android.graphics.Bitmap
import com.richard.retrohall.domain.game.LocalGame
import com.richard.retrohall.domain.input.GameAction
import com.richard.retrohall.domain.save.SaveSlot
import kotlinx.coroutines.flow.StateFlow

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

    /**
     * 输出帧流：真实核心会话持续发布最新游戏画面，演示会话为 null。
     * 显示层依赖它绘制游戏画面。
     */
    val frames: StateFlow<Bitmap?>

    /** 调整游戏速度（0.5f ~ 2.0f），1f 为原始速度。演示会话为空实现。 */
    fun setGameSpeed(speed: Float) = Unit
}
