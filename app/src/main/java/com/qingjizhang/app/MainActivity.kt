package com.qingjizhang.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.qingjizhang.app.data.AppSettings
import com.qingjizhang.app.ui.LocalApp
import com.qingjizhang.app.ui.nav.QingJiZhangRoot
import com.qingjizhang.app.ui.theme.QingJiZhangTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = application as QingJiZhangApp
        setContent {
            val settings by app.container.settings.settings.collectAsState(initial = AppSettings())
            CompositionLocalProvider(LocalApp provides app.container) {
                QingJiZhangTheme(darkTheme = settings.darkTheme) {
                    QingJiZhangRoot()
                }
            }
        }
    }
}
