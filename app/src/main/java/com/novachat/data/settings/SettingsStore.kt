package com.novachat.data.settings

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore("novachat_settings")

/** App-wide settings. maxTokens uses 0 = "provider default" (unset). */
class SettingsStore(private val context: Context) {

    val temperature: Flow<Float> = context.dataStore.data
        .map { it[TEMPERATURE] ?: 0.7f }

    val maxTokens: Flow<Int?> = context.dataStore.data
        .map { (it[MAX_TOKENS] ?: 0).takeIf { v -> v > 0 } }

    val themeMode: Flow<String> = context.dataStore.data
        .map { it[THEME_MODE] ?: THEME_SYSTEM }

    val dynamicColor: Flow<Boolean> = context.dataStore.data
        .map { it[DYNAMIC_COLOR] ?: true }

    suspend fun setTemperature(value: Float) {
        context.dataStore.edit { it[TEMPERATURE] = value.coerceIn(0f, 2f) }
    }

    suspend fun setMaxTokens(value: Int?) {
        context.dataStore.edit { it[MAX_TOKENS] = value ?: 0 }
    }

    suspend fun setThemeMode(mode: String) {
        context.dataStore.edit { it[THEME_MODE] = mode }
    }

    suspend fun setDynamicColor(enabled: Boolean) {
        context.dataStore.edit { it[DYNAMIC_COLOR] = enabled }
    }

    companion object {
        const val THEME_SYSTEM = "system"
        const val THEME_LIGHT = "light"
        const val THEME_DARK = "dark"

        private val TEMPERATURE = floatPreferencesKey("temperature")
        private val MAX_TOKENS = intPreferencesKey("max_tokens")
        private val THEME_MODE = stringPreferencesKey("theme_mode")
        private val DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
    }
}
