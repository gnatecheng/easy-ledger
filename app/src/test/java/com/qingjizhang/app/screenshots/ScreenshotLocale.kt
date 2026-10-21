package com.qingjizhang.app.screenshots

import android.content.Context
import android.content.res.Configuration
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.core.os.LocaleListCompat
import com.qingjizhang.app.data.AppLanguage
import com.qingjizhang.app.ui.i18n.LocaleHelper
import com.qingjizhang.app.ui.theme.QingJiZhangTheme
import org.robolectric.RuntimeEnvironment
import java.util.Locale

object ScreenshotLocale {
    fun applyAppLocale(localeTag: String) {
        val language = when {
            localeTag.startsWith("zh") -> AppLanguage.ZH
            else -> AppLanguage.EN
        }
        LocaleHelper.applyAppLanguage(language)
        val tags = when (language) {
            AppLanguage.ZH -> "zh-CN"
            AppLanguage.EN -> "en"
            AppLanguage.SYSTEM -> ""
        }
        if (tags.isNotEmpty()) {
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(tags))
        }
    }

    fun applyRobolectricEnvironment(localeTag: String, darkTheme: Boolean) {
        val localePart = when {
            localeTag.startsWith("zh") -> "zh-rCN"
            else -> "en-rUS"
        }
        RuntimeEnvironment.setQualifiers("$localePart-w411dp-h822dp-port-xxhdpi")
        RuntimeEnvironment.setQualifiers(if (darkTheme) "+night" else "+notnight")
        applyAppLocale(localeTag)
    }

    fun localeForTag(localeTag: String): Locale =
        if (localeTag.startsWith("zh")) Locale.SIMPLIFIED_CHINESE else Locale.US

    @Composable
    fun Host(
        baseContext: Context,
        localeTag: String,
        darkTheme: Boolean,
        content: @Composable () -> Unit,
    ) {
        val locale = localeForTag(localeTag)
        val configuration = Configuration(baseContext.resources.configuration).apply {
            setLocale(locale)
            setLocales(android.os.LocaleList.forLanguageTags(locale.toLanguageTag()))
            uiMode = (uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or
                if (darkTheme) Configuration.UI_MODE_NIGHT_YES else Configuration.UI_MODE_NIGHT_NO
        }
        val localizedContext = baseContext.createConfigurationContext(configuration)
        CompositionLocalProvider(
            LocalContext provides localizedContext,
            LocalConfiguration provides configuration,
        ) {
            QingJiZhangTheme(darkTheme = darkTheme) {
                content()
            }
        }
    }
}
