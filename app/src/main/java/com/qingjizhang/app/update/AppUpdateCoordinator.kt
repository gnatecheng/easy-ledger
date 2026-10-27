package com.qingjizhang.app.update

import android.app.Activity
import android.content.Context
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import com.qingjizhang.app.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object AppUpdateCoordinator {

    sealed interface Outcome {
        data object UpToDate : Outcome
        data class SideloadUpdate(val remote: RemoteVersionInfo) : Outcome
        data object PlayUpdateStarted : Outcome
        data class Message(val kind: MessageKind) : Outcome
    }

    enum class MessageKind {
        NetworkError,
        ParseError,
        PlayUnavailable,
        GenericError,
    }

    suspend fun checkForUpdates(
        context: Context,
        playUpdateLauncher: ActivityResultLauncher<IntentSenderRequest>? = null,
    ): Outcome {
        return if (context.isInstalledFromPlayStore()) {
            checkPlay(context, playUpdateLauncher)
        } else {
            checkSideload()
        }
    }

    private suspend fun checkPlay(
        context: Context,
        playUpdateLauncher: ActivityResultLauncher<IntentSenderRequest>?,
    ): Outcome {
        val activity = context as? Activity
            ?: return Outcome.Message(MessageKind.PlayUnavailable)
        val launcher = playUpdateLauncher
            ?: return Outcome.Message(MessageKind.PlayUnavailable)
        return when (val result = PlayUpdateChecker(activity, launcher).checkAndPrompt()) {
            PlayUpdateChecker.Result.UpToDate -> Outcome.UpToDate
            PlayUpdateChecker.Result.UpdateStarted -> Outcome.PlayUpdateStarted
            is PlayUpdateChecker.Result.Failed -> when (result.kind) {
                PlayUpdateChecker.FailureKind.PlayUnavailable ->
                    Outcome.Message(MessageKind.PlayUnavailable)
                PlayUpdateChecker.FailureKind.Other ->
                    Outcome.Message(MessageKind.GenericError)
            }
        }
    }

    private suspend fun checkSideload(): Outcome = withContext(Dispatchers.IO) {
        when (val result = SideloadUpdateChecker.check(BuildConfig.VERSION_CODE)) {
            SideloadUpdateChecker.Result.UpToDate -> Outcome.UpToDate
            is SideloadUpdateChecker.Result.UpdateAvailable -> Outcome.SideloadUpdate(result.remote)
            is SideloadUpdateChecker.Result.Failed -> when (result.kind) {
                SideloadUpdateChecker.FailureKind.Network -> Outcome.Message(MessageKind.NetworkError)
                SideloadUpdateChecker.FailureKind.Parse -> Outcome.Message(MessageKind.ParseError)
            }
        }
    }
}
