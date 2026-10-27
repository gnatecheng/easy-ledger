package com.qingjizhang.app.update

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RemoteVersionInfo(
    val versionCode: Int,
    val versionName: String,
    val apkUrl: String? = null,
    val releaseNotes: ReleaseNotes? = null,
)

@Serializable
data class ReleaseNotes(
    @SerialName("zh") val zh: String? = null,
    @SerialName("en") val en: String? = null,
)

fun RemoteVersionInfo.resolveApkUrl(): String {
    val explicit = apkUrl?.trim()?.takeIf { it.isNotEmpty() }
    if (explicit != null) return explicit
    return "https://dl.etais.dev/easy-ledger-$versionName.apk"
}

fun ReleaseNotes?.localizedNote(isChineseUi: Boolean): String? {
    if (this == null) return null
    val primary = if (isChineseUi) zh else en
    val fallback = if (isChineseUi) en else zh
    return primary?.trim()?.takeIf { it.isNotEmpty() }
        ?: fallback?.trim()?.takeIf { it.isNotEmpty() }
}
