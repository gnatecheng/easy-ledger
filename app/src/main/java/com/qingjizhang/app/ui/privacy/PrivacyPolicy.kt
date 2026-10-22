package com.qingjizhang.app.ui.privacy

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import kotlinx.coroutines.delay
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FirstLaunchPrivacyDialog(
    onAcknowledge: () -> Unit,
    onReadFull: () -> Unit,
) {
    val title = stringResource(R.string.privacy_notice_title)
    val focusRequester = remember { FocusRequester() }

    BackHandler(onBack = onAcknowledge)

    BasicAlertDialog(
        onDismissRequest = onAcknowledge,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
    ) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            tonalElevation = 6.dp,
            shadowElevation = 6.dp,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(
                    title,
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier
                        .focusRequester(focusRequester)
                        .focusable()
                        .semantics { contentDescription = title },
                )
                Text(
                    stringResource(R.string.privacy_notice_body),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextButton(onClick = onReadFull) {
                        Text(stringResource(R.string.privacy_notice_read_full))
                    }
                    TextButton(onClick = onAcknowledge) {
                        Text(stringResource(R.string.privacy_notice_got_it))
                    }
                }
            }
        }
    }

    LaunchedEffect(focusRequester) {
        delay(1)
        focusRequester.requestFocus()
    }
}

@Composable
internal fun PrivacyPolicyFormattedBody(raw: String, modifier: Modifier = Modifier) {
    val blocks = remember(raw) { parsePrivacyBlocks(raw) }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        blocks.forEach { block ->
            when (block) {
                is PrivacyBlock.Headline -> Text(
                    block.text,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                is PrivacyBlock.Paragraph -> Text(
                    block.text,
                    style = MaterialTheme.typography.bodyMedium,
                )
                is PrivacyBlock.Bullets -> Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    block.items.forEach { item ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("•", style = MaterialTheme.typography.bodyMedium)
                            Text(item, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

internal sealed class PrivacyBlock {
    data class Headline(val text: String) : PrivacyBlock()
    data class Paragraph(val text: String) : PrivacyBlock()
    data class Bullets(val items: List<String>) : PrivacyBlock()
}

internal fun parsePrivacyBlocks(raw: String): List<PrivacyBlock> {
    val lines = raw.lines()
    val out = mutableListOf<PrivacyBlock>()
    var bulletBuffer = mutableListOf<String>()
    var skipTitleBlock = true

    fun flushBullets() {
        if (bulletBuffer.isNotEmpty()) {
            out += PrivacyBlock.Bullets(bulletBuffer.toList())
            bulletBuffer = mutableListOf()
        }
    }

    for (line in lines) {
        val trimmed = line.trim()
        if (trimmed.isEmpty()) {
            flushBullets()
            continue
        }
        if (skipTitleBlock) {
            if (trimmed.startsWith("·")) {
                skipTitleBlock = false
            } else {
                continue
            }
        }
        if (trimmed.startsWith("·")) {
            bulletBuffer += trimmed.removePrefix("·").trim()
            continue
        }
        flushBullets()
        if (trimmed.length <= 48 && !trimmed.contains("http") && !trimmed.contains("：") && !trimmed.contains(":")) {
            out += PrivacyBlock.Headline(trimmed)
        } else {
            out += PrivacyBlock.Paragraph(trimmed)
        }
    }
    flushBullets()
    return out
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
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            PrivacyPolicyFormattedBody(body)
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
