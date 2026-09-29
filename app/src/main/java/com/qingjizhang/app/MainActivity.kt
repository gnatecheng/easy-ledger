package com.qingjizhang.app

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.qingjizhang.app.data.AppSettings
import com.qingjizhang.app.data.ThemeMode
import com.qingjizhang.app.ui.LocalApp
import com.qingjizhang.app.ui.nav.QingJiZhangRoot
import com.qingjizhang.app.ui.theme.QingJiZhangTheme

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = application as QingJiZhangApp
        setContent {
            val settings by app.container.settings.settings.collectAsState(initial = AppSettings())
            val darkTheme = when (settings.themeMode) {
                ThemeMode.DARK -> true
                ThemeMode.LIGHT -> false
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
            }
            CompositionLocalProvider(LocalApp provides app.container) {
                QingJiZhangTheme(darkTheme = darkTheme) {
                    QingJiZhangRoot()
                }
            }
        }
    }
}
