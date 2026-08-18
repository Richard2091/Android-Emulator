package com.richard.retrohall.domain.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UserSettingsTest {
    @Test
    fun defaultSettingsSupportImmediatePlay() {
        val settings = UserSettings()

        assertEquals(AspectRatio.Original, settings.aspectRatio)
        assertFalse(settings.filterEnabled)
        assertTrue(settings.audioEnabled)
        assertEquals(0.8f, settings.volume)
        assertEquals(VirtualPadVisibility.Visible, settings.virtualPadVisibility)
        assertTrue(settings.virtualPadVisible)
        assertEquals(0.7f, settings.virtualPadOpacity)
        assertEquals(1.0f, settings.virtualPadScale)
        assertEquals(ControlMode.VirtualPad, settings.controlMode)
        assertTrue(settings.hideVirtualPadWhenGamepadConnected)
        assertTrue(settings.autoSaveStateEnabled)
    }
}
