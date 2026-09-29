package com.qingjizhang.app.ui.i18n

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.qingjizhang.app.data.AppLanguage
import com.qingjizhang.app.data.SettingsStore
import java.util.Locale

object LocaleHelper {
    private const val PREFS_BOOT = "locale_boot"
    private const val KEY_LANGUAGE = "app_language"

    fun resolveLocale(language: AppLanguage, systemLocale: Locale = Locale.getDefault()): Locale {
        return when (language) {
            AppLanguage.ZH -> Locale.SIMPLIFIED_CHINESE
            AppLanguage.EN -> Locale.ENGLISH
            AppLanguage.SYSTEM -> if (systemLocale.language.startsWith("en")) Locale.ENGLISH else Locale.SIMPLIFIED_CHINESE
        }
    }

    fun defaultLanguageForSystem(systemLocale: Locale = Locale.getDefault()): AppLanguage =
        if (systemLocale.language.startsWith("en")) AppLanguage.EN else AppLanguage.ZH

    /** Maps AppCompat / system per-app locales to our setting (empty list = follow system). */
    fun appLanguageFromApplicationLocales(locales: LocaleListCompat = AppCompatDelegate.getApplicationLocales()): AppLanguage {
        if (locales.isEmpty) return AppLanguage.SYSTEM
        val tag = locales[0]?.toLanguageTag()?.lowercase(Locale.ROOT).orEmpty()
        return when {
            tag.startsWith("en") -> AppLanguage.EN
            tag.startsWith("zh") -> AppLanguage.ZH
            else -> AppLanguage.SYSTEM
        }
    }

    /** User-initiated or pre-API-33 apply only — do not call when syncing from system on API 33+. */
    fun applyAppLanguage(language: AppLanguage) {
        val tag = when (language) {
            AppLanguage.SYSTEM -> ""
            AppLanguage.ZH -> "zh-CN"
            AppLanguage.EN -> "en"
        }
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(tag))
    }

    fun usesSystemLocaleAsSourceOfTruth(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

    /**
     * Cold start before DataStore: on API 33+ mirror system locales into boot prefs only;
     * on older APIs apply stored boot preference.
     */
    fun syncFromBlocking(context: Context): AppLanguage {
        if (usesSystemLocaleAsSourceOfTruth()) {
            val fromSystem = appLanguageFromApplicationLocales()
            persistForBoot(context, fromSystem)
            return fromSystem
        }
        val stored = context.getSharedPreferences(PREFS_BOOT, Context.MODE_PRIVATE)
            .getString(KEY_LANGUAGE, null)
        val mode = AppLanguage.fromStorage(stored)
        applyAppLanguage(mode)
        return mode
    }

    /** API 33+: read system per-app language and persist to DataStore + boot without writing back to the OS. */
    suspend fun syncSystemLocalesIntoAppStorage(context: Context, settings: SettingsStore) {
        if (!usesSystemLocaleAsSourceOfTruth()) return
        val fromSystem = appLanguageFromApplicationLocales()
        persistForBoot(context, fromSystem)
        settings.update { current ->
            if (current.appLanguage == fromSystem) current else current.copy(appLanguage = fromSystem)
        }
    }

    fun persistForBoot(context: Context, language: AppLanguage) {
        context.applicationContext.getSharedPreferences(PREFS_BOOT, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_LANGUAGE, language.storageKey)
            .apply()
    }

    fun wrapContext(base: Context, locale: Locale): Context {
        val config = Configuration(base.resources.configuration)
        config.setLocale(locale)
        return base.createConfigurationContext(config)
    }
}
