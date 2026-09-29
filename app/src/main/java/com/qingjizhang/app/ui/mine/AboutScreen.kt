package com.qingjizhang.app.ui.mine

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.qingjizhang.app.BuildConfig
import com.qingjizhang.app.R
import com.qingjizhang.app.ui.components.AppCard

private const val GITHUB_URL = "https://github.com/gnatecheng/qingjizhang"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.about_title)) },
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            AppCard {
                Text(stringResource(R.string.about_title), fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(8.dp))
                Text(
                    stringResource(R.string.about_tagline),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            AppCard {
                Text(stringResource(R.string.about_version), color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    stringResource(R.string.about_version_value, BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE),
                    fontWeight = FontWeight.Medium,
                )
                Spacer(Modifier.height(12.dp))
                Text(stringResource(R.string.about_last_update), color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(formatBuildTime(BuildConfig.BUILD_TIME_ISO), fontWeight = FontWeight.Medium)
            }
            AppCard {
                Text(stringResource(R.string.about_github), fontWeight = FontWeight.Medium)
                Spacer(Modifier.height(4.dp))
                Text(GITHUB_URL, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(12.dp))
                OutlinedButton(
                    onClick = {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(GITHUB_URL)))
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.about_open_github))
                }
            }
        }
    }
}

@Composable
fun formatBuildTime(iso: String): String {
    if (iso.isBlank()) return stringResource(R.string.about_build_time_unknown)
    val parsed = runCatching { java.time.ZonedDateTime.parse(iso) }.getOrNull()
    return if (parsed != null) formatBuildTimeParsed(parsed) else iso
}

@Composable
private fun formatBuildTimeParsed(instant: java.time.ZonedDateTime): String {
    val locale = androidx.compose.ui.platform.LocalConfiguration.current.locales[0]
    val pattern = if (locale.language.startsWith("en")) "MMM d, yyyy HH:mm z" else "yyyy年M月d日 HH:mm z"
    return instant.withZoneSameInstant(java.time.ZoneId.systemDefault())
        .format(java.time.format.DateTimeFormatter.ofPattern(pattern, locale))
}
