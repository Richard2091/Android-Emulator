package com.richard.retrohall.data.settings

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.richard.retrohall.domain.settings.AspectRatio
import com.richard.retrohall.domain.settings.UserSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.userSettingsDataStore by preferencesDataStore(name = "user_settings")

class UserSettingsStore(private val context: Context) {
    val settings: Flow<UserSettings> = context.userSettingsDataStore.data.map { preferences ->
        UserSettings(
            aspectRatio = preferences[Keys.AspectRatio]?.let { AspectRatio.valueOf(it) } ?: AspectRatio.Original,
            filterEnabled = preferences[Keys.FilterEnabled] ?: false,
            audioEnabled = preferences[Keys.AudioEnabled] ?: true,
            volume = preferences[Keys.Volume] ?: 0.8f,
            virtualPadVisible = preferences[Keys.VirtualPadVisible] ?: true,
            virtualPadOpacity = preferences[Keys.VirtualPadOpacity] ?: 0.7f,
            virtualPadScale = preferences[Keys.VirtualPadScale] ?: 1.0f,
            hideVirtualPadWhenGamepadConnected = preferences[Keys.HideVirtualPadWhenGamepadConnected] ?: true,
            autoSaveStateEnabled = preferences[Keys.AutoSaveStateEnabled] ?: true,
        )
    }

    suspend fun update(settings: UserSettings) {
        context.userSettingsDataStore.edit { preferences ->
            preferences[Keys.AspectRatio] = settings.aspectRatio.name
            preferences[Keys.FilterEnabled] = settings.filterEnabled
            preferences[Keys.AudioEnabled] = settings.audioEnabled
            preferences[Keys.Volume] = settings.volume.coerceIn(0f, 1f)
            preferences[Keys.VirtualPadVisible] = settings.virtualPadVisible
            preferences[Keys.VirtualPadOpacity] = settings.virtualPadOpacity.coerceIn(0.1f, 1f)
            preferences[Keys.VirtualPadScale] = settings.virtualPadScale.coerceIn(0.5f, 2f)
            preferences[Keys.HideVirtualPadWhenGamepadConnected] = settings.hideVirtualPadWhenGamepadConnected
            preferences[Keys.AutoSaveStateEnabled] = settings.autoSaveStateEnabled
        }
    }

    private object Keys {
        val AspectRatio = stringPreferencesKey("aspect_ratio")
        val FilterEnabled = booleanPreferencesKey("filter_enabled")
        val AudioEnabled = booleanPreferencesKey("audio_enabled")
        val Volume = floatPreferencesKey("volume")
        val VirtualPadVisible = booleanPreferencesKey("virtual_pad_visible")
        val VirtualPadOpacity = floatPreferencesKey("virtual_pad_opacity")
        val VirtualPadScale = floatPreferencesKey("virtual_pad_scale")
        val HideVirtualPadWhenGamepadConnected = booleanPreferencesKey("hide_virtual_pad_when_gamepad_connected")
        val AutoSaveStateEnabled = booleanPreferencesKey("auto_save_state_enabled")
    }
}
