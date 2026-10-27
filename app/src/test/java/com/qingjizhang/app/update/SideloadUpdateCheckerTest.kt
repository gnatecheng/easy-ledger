package com.qingjizhang.app.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SideloadUpdateCheckerTest {

    @Test
    fun compare_newerRemote_returnsUpdateAvailable() {
        val remote = RemoteVersionInfo(versionCode = 8, versionName = "1.3.4")
        val result = SideloadUpdateChecker.compare(7, remote)
        assertTrue(result is SideloadUpdateChecker.Result.UpdateAvailable)
        assertEquals(remote, (result as SideloadUpdateChecker.Result.UpdateAvailable).remote)
    }

    @Test
    fun compare_sameOrOlder_returnsUpToDate() {
        val remote = RemoteVersionInfo(versionCode = 7, versionName = "1.3.3")
        assertEquals(SideloadUpdateChecker.Result.UpToDate, SideloadUpdateChecker.compare(7, remote))
        assertEquals(SideloadUpdateChecker.Result.UpToDate, SideloadUpdateChecker.compare(8, remote))
    }

    @Test
    fun resolveApkUrl_usesExplicitOrFallback() {
        val withUrl = RemoteVersionInfo(8, "1.3.4", apkUrl = "https://example.com/app.apk")
        assertEquals("https://example.com/app.apk", withUrl.resolveApkUrl())
        val fallback = RemoteVersionInfo(8, "1.3.4")
        assertEquals("https://dl.etais.dev/easy-ledger-1.3.4.apk", fallback.resolveApkUrl())
    }

    @Test
    fun localizedNote_prefersLocaleWithFallback() {
        val notes = ReleaseNotes(zh = "中文说明", en = "English notes")
        assertEquals("中文说明", notes.localizedNote(isChineseUi = true))
        assertEquals("English notes", notes.localizedNote(isChineseUi = false))
    }
}
