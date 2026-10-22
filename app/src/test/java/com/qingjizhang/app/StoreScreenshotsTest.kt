package com.qingjizhang.app

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onRoot
import com.github.takahirom.roborazzi.captureRoboImage
import com.qingjizhang.app.domain.AppClock
import com.qingjizhang.app.screenshots.ScreenshotLocale
import com.qingjizhang.app.ui.LocalApp
import com.qingjizhang.app.ui.budget.BudgetScreen
import com.qingjizhang.app.ui.home.HomeScreen
import com.qingjizhang.app.ui.mine.MineScreen
import com.qingjizhang.app.ui.mine.SettingsScreen
import com.qingjizhang.app.ui.privacy.FirstLaunchPrivacyDialog
import com.qingjizhang.app.ui.privacy.PrivacyPolicyScreen
import com.qingjizhang.app.ui.stats.StatsScreen
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
@Config(sdk = [35], application = QingJiZhangApp::class)
@LooperMode(LooperMode.Mode.PAUSED)
class StoreScreenshotsTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ScreenshotTestActivity>()

    private fun appContainer(): AppContainer =
        (org.robolectric.RuntimeEnvironment.getApplication() as QingJiZhangApp).container

    @Before
    fun seedFixedDemo() {
        AppClock.todayOverride = LocalDate.of(2026, 9, 28)
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

    private fun runShot(
        localeTag: String,
        darkTheme: Boolean,
        relativePath: String,
        content: @Composable () -> Unit,
    ) {
        ScreenshotLocale.applyRobolectricEnvironment(localeTag, darkTheme)
        composeRule.setContent {
            ScreenshotLocale.Host(
                baseContext = composeRule.activity,
                localeTag = localeTag,
                darkTheme = darkTheme,
                content = content,
            )
        }
        captureTo(relativePath)
    }

    @Test
    fun fastlane_en_home() = fastlaneTab("en-US", false, { HomeScreenContent() }, "1_home.png")

    @Test
    fun fastlane_en_txns() = fastlaneTab("en-US", false, { TxnsScreenContent() }, "2_txns.png")

    @Test
    fun fastlane_en_stats() = fastlaneTab("en-US", false, { StatsScreen() }, "3_stats.png")

    @Test
    fun fastlane_en_budgets() = fastlaneTab("en-US", false, { BudgetScreen() }, "4_budgets.png")

    @Test
    fun fastlane_en_mine() = fastlaneTab("en-US", false, { MineScreenContent() }, "5_mine.png")

    @Test
    fun fastlane_zh_home() = fastlaneTab("zh-CN", false, { HomeScreenContent() }, "1_home.png")

    @Test
    fun fastlane_zh_txns() = fastlaneTab("zh-CN", false, { TxnsScreenContent() }, "2_txns.png")

    @Test
    fun fastlane_zh_stats() = fastlaneTab("zh-CN", false, { StatsScreen() }, "3_stats.png")

    @Test
    fun fastlane_zh_budgets() = fastlaneTab("zh-CN", false, { BudgetScreen() }, "4_budgets.png")

    @Test
    fun fastlane_zh_mine() = fastlaneTab("zh-CN", false, { MineScreenContent() }, "5_mine.png")

    private fun fastlaneTab(
        localeFolder: String,
        darkTheme: Boolean,
        screen: @Composable () -> Unit,
        fileName: String,
    ) {
        val app = appContainer()
        val localeTag = if (localeFolder.startsWith("zh")) "zh-CN" else "en-US"
        runShot(localeTag, darkTheme, "fastlane/metadata/android/$localeFolder/images/phoneScreenshots/$fileName") {
            CompositionLocalProvider(LocalApp provides app) {
                screen()
            }
        }
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
        runShot("zh-CN", darkTheme = false, "docs/screenshots/play-readiness/settings_privacy_zh_light.png") {
            CompositionLocalProvider(LocalApp provides app) {
                SettingsScreen(onBack = {}, onPrivacy = {})
            }
        }
    }

    @Test
    fun playReadiness_privacyNotice_zhLight() {
        val app = appContainer()
        runShot("zh-CN", darkTheme = false, "docs/screenshots/play-readiness/privacy_notice_zh_light.png") {
            CompositionLocalProvider(LocalApp provides app) {
                Box(Modifier.fillMaxSize()) {
                    HomeScreenContent()
                    FirstLaunchPrivacyDialog(onAcknowledge = {}, onReadFull = {})
                }
            }
        }
    }

    @Test
    fun playReadiness_privacyNotice_enDark() {
        val app = appContainer()
        runShot("en", darkTheme = true, "docs/screenshots/play-readiness/privacy_notice_en_dark.png") {
            CompositionLocalProvider(LocalApp provides app) {
                Box(Modifier.fillMaxSize()) {
                    HomeScreenContent()
                    FirstLaunchPrivacyDialog(onAcknowledge = {}, onReadFull = {})
                }
            }
        }
    }

    @Test
    fun playReadiness_privacyPolicy_en() {
        runShot("en", darkTheme = false, "docs/screenshots/play-readiness/privacy_policy_en.png") {
            PrivacyPolicyScreen(onBack = {})
        }
    }

    @Test
    fun playReadiness_privacyPolicy_zh() {
        runShot("zh-CN", darkTheme = false, "docs/screenshots/play-readiness/privacy_policy_zh.png") {
            PrivacyPolicyScreen(onBack = {})
        }
    }
}
