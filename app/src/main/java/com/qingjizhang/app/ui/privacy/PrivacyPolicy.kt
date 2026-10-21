package com.qingjizhang.app.ui.privacy

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.qingjizhang.app.R
import com.qingjizhang.app.ui.i18n.isEnglishUi

object PrivacyPolicyUrls {
    const val ZH = "https://etais.dev/privacy/"
    const val EN = "https://etais.dev/en/privacy/"
}

@Composable
fun privacyPolicyWebUrl(): String =
    if (isEnglishUi()) PrivacyPolicyUrls.EN else PrivacyPolicyUrls.ZH

@Composable
private fun privacyPolicyAssetName(): String =
    if (isEnglishUi()) "privacy/privacy_en.txt" else "privacy/privacy_zh-CN.txt"

@Composable
fun rememberPrivacyPolicyText(): String {
    val context = LocalContext.current
    val asset = privacyPolicyAssetName()
    return remember(asset) {
        runCatching {
            context.assets.open(asset).bufferedReader().use { it.readText() }
        }.getOrElse { "" }
    }
}

@Composable
fun FirstLaunchPrivacyDialog(
    onAcknowledge: () -> Unit,
    onReadFull: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = { /* not a consent gate */ },
        title = { Text(stringResource(R.string.privacy_notice_title)) },
        text = { Text(stringResource(R.string.privacy_notice_body)) },
        confirmButton = {
            TextButton(onClick = onAcknowledge) {
                Text(stringResource(R.string.privacy_notice_got_it))
            }
        },
        dismissButton = {
            TextButton(onClick = onReadFull) {
                Text(stringResource(R.string.privacy_notice_read_full))
            }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyPolicyScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val body = rememberPrivacyPolicyText()
    val webUrl = privacyPolicyWebUrl()
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.privacy_policy_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.action_back))
                    }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(body, style = MaterialTheme.typography.bodyMedium)
            OutlinedButton(
                onClick = {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(webUrl)))
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.privacy_open_web))
            }
        }
    }
}
