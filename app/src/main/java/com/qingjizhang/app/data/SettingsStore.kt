package com.qingjizhang.app.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking

private val Context.dataStore by preferencesDataStore("qingjizhang_settings")

data class AppSettings(
    val largeTxnThresholdCents: Long = 100_000L,
    val inactivityNudgeDays: Int = 3,
    val demoSeeded: Boolean = false,
    val nudgeDismissedAt: Long = 0L,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val appLanguage: AppLanguage = AppLanguage.SYSTEM,
)

class SettingsStore(context: Context) {
    private val ds = context.applicationContext.dataStore

    val settings: Flow<AppSettings> = ds.data.map { it.toSettings() }

    suspend fun update(transform: (AppSettings) -> AppSettings) {
        ds.edit { prefs ->
            val next = transform(prefs.toSettings())
            prefs[Keys.THRESHOLD] = next.largeTxnThresholdCents
            prefs[Keys.INACTIVITY] = next.inactivityNudgeDays
            prefs[Keys.DEMO] = next.demoSeeded
            prefs[Keys.NUDGE] = next.nudgeDismissedAt
            prefs[Keys.THEME_MODE] = next.themeMode.storageKey
            prefs[Keys.APP_LANGUAGE] = next.appLanguage.storageKey
        }
    }

    fun readBlocking(): AppSettings = runBlocking { settings.first() }

    private object Keys {
        val THRESHOLD = longPreferencesKey("large_txn_threshold_cents")
        val INACTIVITY = intPreferencesKey("inactivity_nudge_days")
        val DEMO = booleanPreferencesKey("demo_seeded")
        val NUDGE = longPreferencesKey("nudge_dismissed_at")
        val DARK = booleanPreferencesKey("dark_theme")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val APP_LANGUAGE = stringPreferencesKey("app_language")
    }

    private fun Preferences.toSettings(): AppSettings {
        val themeMode = this[Keys.THEME_MODE]?.let(ThemeMode::fromStorage)
            ?: this[Keys.DARK]?.let { ThemeMode.fromLegacyDarkFlag(it) }
            ?: ThemeMode.SYSTEM
        val language = AppLanguage.fromStorage(this[Keys.APP_LANGUAGE])
        return AppSettings(
            largeTxnThresholdCents = this[Keys.THRESHOLD] ?: 100_000L,
            inactivityNudgeDays = this[Keys.INACTIVITY] ?: 3,
            demoSeeded = this[Keys.DEMO] ?: false,
            nudgeDismissedAt = this[Keys.NUDGE] ?: 0L,
            themeMode = themeMode,
            appLanguage = language,
        )
    }

    companion object {
        fun readLanguageBlocking(context: Context): AppLanguage =
            SettingsStore(context).readBlocking().appLanguage
    }
}
