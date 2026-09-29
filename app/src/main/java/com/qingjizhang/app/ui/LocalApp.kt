package com.qingjizhang.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidedValue
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.qingjizhang.app.AppContainer
import com.qingjizhang.app.R

private val LocalAppContainer = staticCompositionLocalOf<AppContainer?> { null }

object LocalApp {
    val current: AppContainer
        @Composable
        @ReadOnlyComposable
        get() = LocalAppContainer.current ?: error(stringResource(R.string.app_container_missing))

    infix fun provides(container: AppContainer): ProvidedValue<AppContainer?> =
        LocalAppContainer provides container
}

fun <VM : ViewModel> vmFactory(create: () -> VM): ViewModelProvider.Factory =
    object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = create() as T
    }
