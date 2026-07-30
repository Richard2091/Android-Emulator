package com.richard.retrohall.input

import android.view.KeyEvent
import com.richard.retrohall.domain.input.GameAction

object KeyEventMapper {
    fun map(keyCode: Int): GameAction? = when (keyCode) {
        KeyEvent.KEYCODE_DPAD_UP -> GameAction.Up
        KeyEvent.KEYCODE_DPAD_DOWN -> GameAction.Down
        KeyEvent.KEYCODE_DPAD_LEFT -> GameAction.Left
        KeyEvent.KEYCODE_DPAD_RIGHT -> GameAction.Right
        KeyEvent.KEYCODE_DPAD_CENTER,
        KeyEvent.KEYCODE_ENTER,
        KeyEvent.KEYCODE_NUMPAD_ENTER,
        KeyEvent.KEYCODE_BUTTON_A -> GameAction.Confirm
        KeyEvent.KEYCODE_BACK,
        KeyEvent.KEYCODE_ESCAPE -> GameAction.Back
        KeyEvent.KEYCODE_MENU,
        KeyEvent.KEYCODE_BUTTON_MODE -> GameAction.Menu
        KeyEvent.KEYCODE_BUTTON_X -> GameAction.NesA
        KeyEvent.KEYCODE_BUTTON_B -> GameAction.NesB
        KeyEvent.KEYCODE_BUTTON_START -> GameAction.Start
        KeyEvent.KEYCODE_BUTTON_SELECT -> GameAction.Select
        else -> null
    }
}
