package com.qingjizhang.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import com.qingjizhang.app.ui.LocalApp
import com.qingjizhang.app.ui.nav.QingJiZhangRoot
import com.qingjizhang.app.ui.theme.QingJiZhangTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = application as QingJiZhangApp
        setContent {
            CompositionLocalProvider(LocalApp provides app.container) {
                QingJiZhangTheme {
                    QingJiZhangRoot()
                }
            }
        }
    }
}
