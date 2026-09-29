package com.qingjizhang.app.ui.i18n

import androidx.core.os.LocaleListCompat
import com.qingjizhang.app.data.AppLanguage
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class LocaleHelperTest {

    @Test
    fun appLanguageFromApplicationLocales_emptyIsSystem() {
        assertEquals(AppLanguage.SYSTEM, LocaleHelper.appLanguageFromApplicationLocales(LocaleListCompat.getEmptyLocaleList()))
    }

    @Test
    fun appLanguageFromApplicationLocales_enAndZh() {
        assertEquals(AppLanguage.EN, LocaleHelper.appLanguageFromApplicationLocales(LocaleListCompat.forLanguageTags("en")))
        assertEquals(AppLanguage.ZH, LocaleHelper.appLanguageFromApplicationLocales(LocaleListCompat.forLanguageTags("zh-CN")))
    }

}
