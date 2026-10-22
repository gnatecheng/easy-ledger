package com.qingjizhang.app.ui.privacy

import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.qingjizhang.app.R
import com.qingjizhang.app.ScreenshotTestActivity
import com.qingjizhang.app.ui.theme.QingJiZhangTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.annotation.LooperMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w411dp-h822dp-port-xxhdpi")
@LooperMode(LooperMode.Mode.PAUSED)
class PrivacyNoticeDialogTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ScreenshotTestActivity>()

    @Test
    fun backPress_acknowledgesAndDismisses() {
        var acknowledged = false
        composeRule.setContent {
            QingJiZhangTheme {
                FirstLaunchPrivacyDialog(
                    onAcknowledge = { acknowledged = true },
                    onReadFull = {},
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.activityRule.scenario.onActivity { activity ->
            activity.onBackPressedDispatcher.onBackPressed()
        }
        composeRule.waitForIdle()
        assertTrue(acknowledged)
    }

    @Test
    fun titleReceivesInitialAccessibilityFocus() {
        composeRule.setContent {
            QingJiZhangTheme {
                FirstLaunchPrivacyDialog(onAcknowledge = {}, onReadFull = {})
            }
        }
        composeRule.waitForIdle()
        val title = composeRule.activity.getString(R.string.privacy_notice_title)
        composeRule.onNodeWithText(title).assertIsFocused()
    }
}
