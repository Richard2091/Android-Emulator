package com.richard.retrohall.data.core

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.coreSelectionDataStore by preferencesDataStore(name = "core_selection")

/**
 * 记录每个平台当前选择的核心 ID（DataStore）。
 */
class CoreSelectionStore(private val context: Context) {
    val selections: Flow<Map<String, String>> = context.coreSelectionDataStore.data.map { preferences ->
        preferences.asMap().mapNotNull { (key, value) ->
            val platform = key.name.removePrefix(Keys.KEY_PREFIX).takeIf { it.isNotBlank() } ?: return@mapNotNull null
            platform to value.toString()
        }.toMap()
    }

    suspend fun selectedCoreFor(platformId: String): String? =
        context.coreSelectionDataStore.data.map { it[Keys.of(platformId)] }.first()

    suspend fun select(platformId: String, coreId: String) {
        context.coreSelectionDataStore.edit { preferences ->
            preferences[Keys.of(platformId)] = coreId
        }
    }

    private object Keys {
        const val KEY_PREFIX = "selected_core_"
        fun of(platformId: String) = stringPreferencesKey("$KEY_PREFIX${platformId.lowercase()}")
    }
}
