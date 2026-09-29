package com.qingjizhang.app.ui.i18n

import android.content.Context
import android.content.res.Configuration
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.qingjizhang.app.data.AppLanguage
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

    fun applyAppLanguage(language: AppLanguage) {
        val tag = when (language) {
            AppLanguage.SYSTEM -> ""
            AppLanguage.ZH -> "zh-CN"
            AppLanguage.EN -> "en"
        }
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(tag))
    }

    /** Sync locale before DataStore is ready (cold start / Android 13+ per-app language). */
    fun syncFromBlocking(context: Context): AppLanguage {
        val stored = context.getSharedPreferences(PREFS_BOOT, Context.MODE_PRIVATE)
            .getString(KEY_LANGUAGE, null)
        val mode = AppLanguage.fromStorage(stored)
        applyAppLanguage(mode)
        return mode
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
