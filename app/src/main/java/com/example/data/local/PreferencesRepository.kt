package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.data.model.ReaderThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.folioPrefsStore: DataStore<Preferences> by preferencesDataStore(name = "folio_reader_prefs")

data class ReaderPreferences(
    val nightMode: Boolean = false,
    val readerTheme: ReaderThemeMode = ReaderThemeMode.PAPER,
    val fontSizeSp: Int = 18,
    val lineSpacingLevel: Int = 1,
    val useSerifFont: Boolean = true
)

class PreferencesRepository(private val context: Context) {
    private object Keys {
        val NIGHT_MODE = booleanPreferencesKey("night_mode")
        val READER_THEME = stringPreferencesKey("reader_theme")
        val FONT_SIZE_SP = intPreferencesKey("font_size_sp")
        val LINE_SPACING_LEVEL = intPreferencesKey("line_spacing_level")
        val USE_SERIF = booleanPreferencesKey("use_serif")
    }

    val preferences: Flow<ReaderPreferences> = context.folioPrefsStore.data.map { prefs ->
        ReaderPreferences(
            nightMode = prefs[Keys.NIGHT_MODE] ?: false,
            readerTheme = ReaderThemeMode.fromRaw(prefs[Keys.READER_THEME]),
            fontSizeSp = (prefs[Keys.FONT_SIZE_SP] ?: 18).coerceIn(14, 28),
            lineSpacingLevel = (prefs[Keys.LINE_SPACING_LEVEL] ?: 1).coerceIn(0, 2),
            useSerifFont = prefs[Keys.USE_SERIF] ?: true
        )
    }

    suspend fun setNightMode(enabled: Boolean, syncReaderTheme: Boolean = true) {
        context.folioPrefsStore.edit { prefs ->
            prefs[Keys.NIGHT_MODE] = enabled
            if (syncReaderTheme) {
                val current = ReaderThemeMode.fromRaw(prefs[Keys.READER_THEME])
                if (enabled && (current == ReaderThemeMode.PAPER || current == ReaderThemeMode.SEPIA)) {
                    prefs[Keys.READER_THEME] = ReaderThemeMode.NIGHT.name
                } else if (!enabled && (current == ReaderThemeMode.NIGHT || current == ReaderThemeMode.OLED)) {
                    prefs[Keys.READER_THEME] = ReaderThemeMode.PAPER.name
                }
            }
        }
    }

    suspend fun setReaderTheme(theme: ReaderThemeMode) {
        context.folioPrefsStore.edit { prefs ->
            prefs[Keys.READER_THEME] = theme.name
            prefs[Keys.NIGHT_MODE] = (theme == ReaderThemeMode.NIGHT || theme == ReaderThemeMode.OLED)
        }
    }

    suspend fun setFontSize(fontSizeSp: Int) {
        context.folioPrefsStore.edit { prefs ->
            prefs[Keys.FONT_SIZE_SP] = fontSizeSp.coerceIn(14, 28)
        }
    }

    suspend fun setLineSpacingLevel(level: Int) {
        context.folioPrefsStore.edit { prefs ->
            prefs[Keys.LINE_SPACING_LEVEL] = level.coerceIn(0, 2)
        }
    }

    suspend fun setUseSerifFont(useSerif: Boolean) {
        context.folioPrefsStore.edit { prefs ->
            prefs[Keys.USE_SERIF] = useSerif
        }
    }
}
