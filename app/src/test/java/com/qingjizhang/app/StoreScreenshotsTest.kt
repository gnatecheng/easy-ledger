package com.qingjizhang.app

import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onRoot
import androidx.core.os.LocaleListCompat
import com.github.takahirom.roborazzi.captureRoboImage
import com.qingjizhang.app.data.Seeder
import com.qingjizhang.app.ui.LocalApp
import com.qingjizhang.app.ui.budget.BudgetScreen
import com.qingjizhang.app.ui.home.HomeScreen
import com.qingjizhang.app.ui.mine.MineScreen
import com.qingjizhang.app.ui.mine.SettingsScreen
import com.qingjizhang.app.ui.privacy.FirstLaunchPrivacyDialog
import com.qingjizhang.app.ui.privacy.PrivacyPolicyScreen
import com.qingjizhang.app.ui.stats.StatsScreen
import com.qingjizhang.app.ui.theme.QingJiZhangTheme
import com.qingjizhang.app.ui.txn.TransactionListScreen
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.annotation.LooperMode
import java.io.File
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], application = QingJiZhangApp::class, qualifiers = "w411dp-h822dp-port-xxhdpi")
@LooperMode(LooperMode.Mode.PAUSED)
class StoreScreenshotsTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ScreenshotTestActivity>()

    private fun appContainer(): AppContainer =
        (org.robolectric.RuntimeEnvironment.getApplication() as QingJiZhangApp).container

    @Before
    fun seedFixedDemo() {
        Seeder.demoTodayOverride = LocalDate.of(2026, 9, 28)
        runBlocking {
            appContainer().seeder.resetDemoData()
        }
    }

    private fun repoRootFile(relativePath: String): File {
        val moduleDir = File(System.getProperty("user.dir"))
        val root = if (moduleDir.name == "app") moduleDir.parentFile!! else moduleDir
        return File(root, relativePath)
    }

    private fun captureTo(relativePath: String) {
        composeRule.waitForIdle()
        val out = repoRootFile(relativePath)
        out.parentFile?.mkdirs()
        composeRule.onRoot().captureRoboImage(out.absolutePath)
    }

    private fun withLocaleAndTheme(
        localeTag: String,
        darkTheme: Boolean,
        content: @Composable () -> Unit,
    ) {
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(localeTag))
        composeRule.setContent {
            QingJiZhangTheme(darkTheme = darkTheme) {
                content()
            }
        }
    }

    @Test
    fun fastlane_en_home() = fastlaneTab("en-US", { HomeScreenContent() }, "1_home.png")

    @Test
    fun fastlane_en_txns() = fastlaneTab("en-US", { TxnsScreenContent() }, "2_txns.png")

    @Test
    fun fastlane_en_stats() = fastlaneTab("en-US", { StatsScreen() }, "3_stats.png")

    @Test
    fun fastlane_en_budgets() = fastlaneTab("en-US", { BudgetScreen() }, "4_budgets.png")

    @Test
    fun fastlane_en_mine() = fastlaneTab("en-US", { MineScreenContent() }, "5_mine.png")

    @Test
    fun fastlane_zh_home() = fastlaneTab("zh-CN", { HomeScreenContent() }, "1_home.png")

    @Test
    fun fastlane_zh_txns() = fastlaneTab("zh-CN", { TxnsScreenContent() }, "2_txns.png")

    @Test
    fun fastlane_zh_stats() = fastlaneTab("zh-CN", { StatsScreen() }, "3_stats.png")

    @Test
    fun fastlane_zh_budgets() = fastlaneTab("zh-CN", { BudgetScreen() }, "4_budgets.png")

    @Test
    fun fastlane_zh_mine() = fastlaneTab("zh-CN", { MineScreenContent() }, "5_mine.png")

    private fun fastlaneTab(
        localeFolder: String,
        screen: @Composable () -> Unit,
        fileName: String,
    ) {
        val app = appContainer()
        withLocaleAndTheme(localeFolder, darkTheme = false) {
            CompositionLocalProvider(LocalApp provides app) {
                screen()
            }
        }
        captureTo("fastlane/metadata/android/$localeFolder/images/phoneScreenshots/$fileName")
    }

    @Composable
    private fun HomeScreenContent() {
        HomeScreen(
            onOpenTxn = {},
            onQuickAdd = {},
            onTransfer = {},
            onOpenTxns = {},
            onOpenBudgets = {},
        )
    }

    @Composable
    private fun TxnsScreenContent() {
        TransactionListScreen(onOpenTxn = {}, onQuickAdd = {}, onTransfer = {})
    }

    @Composable
    private fun MineScreenContent() {
        MineScreen(
            onCategories = {},
            onAccounts = {},
            onRecurring = {},
            onImportExport = {},
            onAppearance = {},
            onSettings = {},
            onAbout = {},
        )
    }

    @Test
    fun playReadiness_settingsPrivacy_zhLight() {
        val app = appContainer()
        withLocaleAndTheme("zh-CN", darkTheme = false) {
            CompositionLocalProvider(LocalApp provides app) {
                SettingsScreen(onBack = {}, onPrivacy = {})
            }
        }
        captureTo("docs/screenshots/play-readiness/settings_privacy_zh_light.png")
    }

    @Test
    fun playReadiness_privacyNotice_zhLight() {
        withLocaleAndTheme("zh-CN", darkTheme = false) {
            FirstLaunchPrivacyDialog(onAcknowledge = {}, onReadFull = {})
        }
        captureTo("docs/screenshots/play-readiness/privacy_notice_zh_light.png")
    }

    @Test
    fun playReadiness_privacyNotice_enDark() {
        withLocaleAndTheme("en", darkTheme = true) {
            FirstLaunchPrivacyDialog(onAcknowledge = {}, onReadFull = {})
        }
        captureTo("docs/screenshots/play-readiness/privacy_notice_en_dark.png")
    }

    @Test
    fun playReadiness_privacyPolicy_en() {
        withLocaleAndTheme("en", darkTheme = false) {
            PrivacyPolicyScreen(onBack = {})
        }
        captureTo("docs/screenshots/play-readiness/privacy_policy_en.png")
    }

    @Test
    fun playReadiness_privacyPolicy_zh() {
        withLocaleAndTheme("zh-CN", darkTheme = false) {
            PrivacyPolicyScreen(onBack = {})
        }
        captureTo("docs/screenshots/play-readiness/privacy_policy_zh.png")
    }
}
