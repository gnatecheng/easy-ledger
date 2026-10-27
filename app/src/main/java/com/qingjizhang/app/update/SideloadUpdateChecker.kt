package com.qingjizhang.app.update

import kotlinx.serialization.json.Json
import java.net.HttpURLConnection
import java.net.URL

object SideloadUpdateChecker {
    const val VERSION_JSON_URL = "https://dl.etais.dev/easy-ledger-version.json"

    private val json = Json { ignoreUnknownKeys = true }

    sealed interface Result {
        data object UpToDate : Result
        data class UpdateAvailable(val remote: RemoteVersionInfo) : Result
        data class Failed(val kind: FailureKind) : Result
    }

    enum class FailureKind {
        Network,
        Parse,
    }

    fun compare(localVersionCode: Int, remote: RemoteVersionInfo): Result {
        return if (remote.versionCode > localVersionCode) {
            Result.UpdateAvailable(remote)
        } else {
            Result.UpToDate
        }
    }

    fun fetchRemoteVersion(
        url: String = VERSION_JSON_URL,
        connectTimeoutMs: Int = 15_000,
        readTimeoutMs: Int = 15_000,
    ): Pair<RemoteVersionInfo?, FailureKind?> {
        val body = runCatching {
            openAndRead(url, connectTimeoutMs, readTimeoutMs)
        }.getOrElse {
            return null to FailureKind.Network
        }
        val remote = runCatching {
            json.decodeFromString<RemoteVersionInfo>(body)
        }.getOrElse {
            return null to FailureKind.Parse
        }
        return remote to null
    }

    fun check(localVersionCode: Int, url: String = VERSION_JSON_URL): Result {
        val (remote, failure) = fetchRemoteVersion(url)
        if (failure != null) return Result.Failed(failure)
        return compare(localVersionCode, remote!!)
    }

    private fun openAndRead(url: String, connectTimeoutMs: Int, readTimeoutMs: Int): String {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = connectTimeoutMs
            readTimeout = readTimeoutMs
            instanceFollowRedirects = true
        }
        try {
            val code = connection.responseCode
            if (code !in 200..299) {
                error("HTTP $code")
            }
            return connection.inputStream.bufferedReader().readText()
        } finally {
            connection.disconnect()
        }
    }
}
