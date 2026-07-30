package com.richard.retrohall.input

import android.view.KeyEvent
import com.richard.retrohall.domain.input.GameAction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class KeyEventMapperTest {
    @Test
    fun mapsNavigationAndSystemKeys() {
        assertEquals(GameAction.Up, KeyEventMapper.map(KeyEvent.KEYCODE_DPAD_UP))
        assertEquals(GameAction.Down, KeyEventMapper.map(KeyEvent.KEYCODE_DPAD_DOWN))
        assertEquals(GameAction.Left, KeyEventMapper.map(KeyEvent.KEYCODE_DPAD_LEFT))
        assertEquals(GameAction.Right, KeyEventMapper.map(KeyEvent.KEYCODE_DPAD_RIGHT))
        assertEquals(GameAction.Confirm, KeyEventMapper.map(KeyEvent.KEYCODE_DPAD_CENTER))
        assertEquals(GameAction.Confirm, KeyEventMapper.map(KeyEvent.KEYCODE_ENTER))
        assertEquals(GameAction.Back, KeyEventMapper.map(KeyEvent.KEYCODE_BACK))
        assertEquals(GameAction.Menu, KeyEventMapper.map(KeyEvent.KEYCODE_MENU))
    }

    @Test
    fun mapsCommonGamepadButtons() {
        assertEquals(GameAction.Confirm, KeyEventMapper.map(KeyEvent.KEYCODE_BUTTON_A))
        assertEquals(GameAction.NesB, KeyEventMapper.map(KeyEvent.KEYCODE_BUTTON_B))
        assertEquals(GameAction.NesA, KeyEventMapper.map(KeyEvent.KEYCODE_BUTTON_X))
        assertEquals(GameAction.Start, KeyEventMapper.map(KeyEvent.KEYCODE_BUTTON_START))
        assertEquals(GameAction.Select, KeyEventMapper.map(KeyEvent.KEYCODE_BUTTON_SELECT))
    }

    @Test
    fun returnsNullForUnsupportedKeys() {
        assertNull(KeyEventMapper.map(KeyEvent.KEYCODE_UNKNOWN))
    }
}
