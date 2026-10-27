package com.qingjizhang.app.update

import android.content.Context
import android.os.Build

private const val PLAY_STORE_PACKAGE = "com.android.vending"

fun Context.isInstalledFromPlayStore(): Boolean {
    val pm = packageManager
    val installer = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        runCatching { pm.getInstallSourceInfo(packageName).installingPackageName }.getOrNull()
    } else {
        @Suppress("DEPRECATION")
        pm.getInstallerPackageName(packageName)
    }
    return installer == PLAY_STORE_PACKAGE
}
