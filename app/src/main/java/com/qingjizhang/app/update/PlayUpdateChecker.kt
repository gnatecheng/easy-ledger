package com.qingjizhang.app.update

import android.app.Activity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.UpdateAvailability
import com.google.android.play.core.ktx.requestAppUpdateInfo

class PlayUpdateChecker(
    private val activity: Activity,
    private val updateLauncher: ActivityResultLauncher<IntentSenderRequest>,
) {
    private val appUpdateManager = AppUpdateManagerFactory.create(activity)

    sealed interface Result {
        data object UpToDate : Result
        data object UpdateStarted : Result
        data class Failed(val kind: FailureKind) : Result
    }

    enum class FailureKind {
        PlayUnavailable,
        Other,
    }

    suspend fun checkAndPrompt(): Result {
        val info = runCatching { appUpdateManager.requestAppUpdateInfo() }.getOrNull()
            ?: return Result.Failed(FailureKind.PlayUnavailable)

        if (info.updateAvailability() != UpdateAvailability.UPDATE_AVAILABLE) {
            return Result.UpToDate
        }

        val updateType = when {
            info.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE) -> AppUpdateType.IMMEDIATE
            info.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE) -> AppUpdateType.FLEXIBLE
            else -> return Result.Failed(FailureKind.PlayUnavailable)
        }

        val options = AppUpdateOptions.newBuilder(updateType).build()
        val started = runCatching {
            appUpdateManager.startUpdateFlowForResult(info, updateLauncher, options)
        }.getOrDefault(false)

        return if (started) Result.UpdateStarted else Result.Failed(FailureKind.Other)
    }
}
