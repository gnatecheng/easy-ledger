package com.qingjizhang.app

import android.graphics.Bitmap
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.core.os.LocaleListCompat
import com.qingjizhang.app.ui.LocalApp
import com.qingjizhang.app.ui.home.HomeScreen
import com.qingjizhang.app.ui.mine.SettingsScreen
import com.qingjizhang.app.ui.privacy.FirstLaunchPrivacyDialog
import com.qingjizhang.app.ui.privacy.PrivacyPolicyScreen
import com.qingjizhang.app.ui.stats.StatsScreen
import com.qingjizhang.app.ui.theme.QingJiZhangTheme
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.io.FileOutputStream

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], application = QingJiZhangApp::class, qualifiers = "w1080dp-h2160dp-480dpi")
@Ignore("Compose captureToImage times out under Robolectric on CI; run locally or use scripts/render_store_screenshots.py")
class StoreScreenshotsTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ScreenshotTestActivity>()

    private fun savePng(relativePath: String) {
        composeRule.waitForIdle()
        val bitmap = composeRule.onNodeWithTag("shot-root").captureToImage().asAndroidBitmap()
        val out = File(relativePath)
        out.parentFile?.mkdirs()
        FileOutputStream(out).use { stream ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        }
    }

    private fun appContainer(): AppContainer =
        (org.robolectric.RuntimeEnvironment.getApplication() as QingJiZhangApp).container

    @Test
    fun settingsPrivacyEntry_lightZh() {
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags("zh-CN"))
        val app = appContainer()
        composeRule.setContent {
            Box(Modifier.testTag("shot-root").fillMaxSize()) {
                QingJiZhangTheme(darkTheme = false) {
                    CompositionLocalProvider(LocalApp provides app) {
                        SettingsScreen(onBack = {}, onPrivacy = {})
                    }
                }
            }
        }
        savePng("docs/screenshots/play-readiness/settings_privacy_zh_light.png")
    }

    @Test
    fun privacyNotice_en_dark() {
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags("en"))
        composeRule.setContent {
            Box(Modifier.testTag("shot-root").fillMaxSize()) {
                QingJiZhangTheme(darkTheme = true) {
                    FirstLaunchPrivacyDialog(onAcknowledge = {}, onReadFull = {})
                }
            }
        }
        savePng("docs/screenshots/play-readiness/privacy_notice_en_dark.png")
    }

    @Test
    fun privacyNotice_zh_light() {
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags("zh-CN"))
        composeRule.setContent {
            Box(Modifier.testTag("shot-root").fillMaxSize()) {
                QingJiZhangTheme(darkTheme = false) {
                    FirstLaunchPrivacyDialog(onAcknowledge = {}, onReadFull = {})
                }
            }
        }
        savePng("docs/screenshots/play-readiness/privacy_notice_zh_light.png")
    }

    @Test
    fun privacyPolicyScreen_en() {
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags("en"))
        composeRule.setContent {
            Box(Modifier.testTag("shot-root").fillMaxSize()) {
                QingJiZhangTheme(darkTheme = false) {
                    PrivacyPolicyScreen(onBack = {})
                }
            }
        }
        savePng("docs/screenshots/play-readiness/privacy_policy_en.png")
    }

    @Test
    fun homeScreen_en_forFastlane() {
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags("en"))
        val app = appContainer()
        composeRule.setContent {
            Box(Modifier.testTag("shot-root").fillMaxSize()) {
                QingJiZhangTheme(darkTheme = false) {
                    CompositionLocalProvider(LocalApp provides app) {
                        HomeScreen(
                            onOpenTxn = {},
                            onQuickAdd = {},
                            onTransfer = {},
                            onOpenTxns = {},
                            onOpenBudgets = {},
                        )
                    }
                }
            }
        }
        savePng("fastlane/metadata/android/en-US/images/phoneScreenshots/1_home.png")
    }

    @Test
    fun statsScreen_zh_forFastlane() {
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags("zh-CN"))
        val app = appContainer()
        composeRule.setContent {
            Box(Modifier.testTag("shot-root").fillMaxSize()) {
                QingJiZhangTheme(darkTheme = false) {
                    CompositionLocalProvider(LocalApp provides app) {
                        StatsScreen()
                    }
                }
            }
        }
        savePng("fastlane/metadata/android/zh-CN/images/phoneScreenshots/1_stats.png")
    }
}
