package com.magictap.widget

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.magictap.R
import com.magictap.appContainer
import com.magictap.ui.AppIcons
import com.magictap.ui.components.EmptyState

/**
 * Placement configuration shared by both widgets. [single] = pick a profile *and* a PC
 * (single-icon widget); otherwise pick just a profile (list widget).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WidgetConfigScreen(
    single: Boolean,
    onPickProfile: (String) -> Unit,
    onPickPc: (profileId: String, pcId: String) -> Unit,
    onOpenApp: () -> Unit,
) {
    val context = LocalContext.current
    val repository = remember { context.appContainer.repository }
    val data by repository.data.collectAsState()
    val ready by repository.ready.collectAsState()

    val nothingToPick = data.profiles.isEmpty() || (single && data.profiles.all { it.pcs.isEmpty() })

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(
                            if (single) R.string.widget_config_pc_title else R.string.widget_config_profile_title,
                        ),
                    )
                },
            )
        },
    ) { padding ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            when {
                !ready -> CircularProgressIndicator(Modifier.align(Alignment.Center))

                nothingToPick -> EmptyState(
                    icon = AppIcons.Power,
                    title = stringResource(R.string.widget_not_configured),
                    description = stringResource(R.string.widget_config_none),
                    actionLabel = stringResource(R.string.widget_open_app),
                    onAction = onOpenApp,
                    modifier = Modifier.align(Alignment.Center),
                )

                else -> LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    for (profile in data.profiles) {
                        if (single) {
                            item(key = "header_${profile.id}") { SectionHeader(profile.alias) }
                            items(profile.pcs, key = { "pc_${it.id}" }) { pc ->
                                PickCard(title = pc.alias, subtitle = pc.mac) { onPickPc(profile.id, pc.id) }
                            }
                        } else {
                            item(key = "profile_${profile.id}") {
                                PickCard(
                                    title = profile.alias,
                                    subtitle = pluralPcs(profile.pcs.size),
                                ) { onPickProfile(profile.id) }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 4.dp, top = 8.dp),
    )
}

@Composable
private fun PickCard(title: String, subtitle: String, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(AppIcons.ChevronRight, contentDescription = null)
        }
    }
}

private fun pluralPcs(count: Int): String = "PC $count"
