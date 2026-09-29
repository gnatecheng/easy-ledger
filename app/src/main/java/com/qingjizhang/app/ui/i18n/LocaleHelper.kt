package com.qingjizhang.app.ui.i18n

import android.content.Context
import android.content.res.Configuration
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.qingjizhang.app.data.AppLanguage
import java.util.Locale

object LocaleHelper {
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

    fun wrapContext(base: Context, locale: Locale): Context {
        val config = Configuration(base.resources.configuration)
        config.setLocale(locale)
        return base.createConfigurationContext(config)
    }
}
