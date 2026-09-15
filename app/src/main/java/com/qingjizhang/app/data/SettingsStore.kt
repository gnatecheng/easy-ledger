package com.qingjizhang.app.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore("qingjizhang_settings")

data class AppSettings(
    val largeTxnThresholdCents: Long = 100_000L,
    val inactivityNudgeDays: Int = 3,
    val demoSeeded: Boolean = false,
    val nudgeDismissedAt: Long = 0L,
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
        }
    }

    private object Keys {
        val THRESHOLD = longPreferencesKey("large_txn_threshold_cents")
        val INACTIVITY = intPreferencesKey("inactivity_nudge_days")
        val DEMO = booleanPreferencesKey("demo_seeded")
        val NUDGE = longPreferencesKey("nudge_dismissed_at")
    }

    private fun Preferences.toSettings() = AppSettings(
        largeTxnThresholdCents = this[Keys.THRESHOLD] ?: 100_000L,
        inactivityNudgeDays = this[Keys.INACTIVITY] ?: 3,
        demoSeeded = this[Keys.DEMO] ?: false,
        nudgeDismissedAt = this[Keys.NUDGE] ?: 0L,
    )
}
