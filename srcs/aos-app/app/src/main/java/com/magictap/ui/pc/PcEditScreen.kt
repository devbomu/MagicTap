package com.magictap.ui.pc

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.magictap.R
import com.magictap.appContainer
import com.magictap.ui.AppIcons
import com.magictap.ui.components.LabeledField
import com.magictap.ui.components.PrimaryButton
import com.magictap.ui.components.SecondaryButton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PcEditScreen(
    profileId: String,
    pcId: String?,
    onDone: () -> Unit,
) {
    val context = LocalContext.current
    val container = remember { context.appContainer }
    val viewModel: PcEditViewModel =
        viewModel(factory = PcEditViewModel.factory(container, profileId, pcId))
    val state by viewModel.state.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(
                            if (state.isNew) R.string.pc_edit_title_new else R.string.pc_edit_title_edit,
                        ),
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onDone) {
                        Icon(AppIcons.Close, contentDescription = stringResource(R.string.action_cancel))
                    }
                },
            )
        },
        bottomBar = {
            Row(
                Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .imePadding()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                SecondaryButton(
                    text = stringResource(R.string.action_cancel),
                    icon = AppIcons.Close,
                    onClick = onDone,
                    modifier = Modifier.weight(1f),
                )
                PrimaryButton(
                    text = stringResource(R.string.action_save),
                    icon = AppIcons.Check,
                    onClick = { viewModel.save(onDone) },
                    modifier = Modifier.weight(1f),
                )
            }
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Spacer(Modifier.height(2.dp))
            LabeledField(
                value = state.alias,
                onValueChange = viewModel::updateAlias,
                label = stringResource(R.string.pc_alias),
                placeholder = stringResource(R.string.pc_alias_hint),
                error = state.aliasError?.let { stringResource(it) },
            )
            LabeledField(
                value = state.mac,
                onValueChange = viewModel::updateMac,
                label = stringResource(R.string.pc_mac),
                placeholder = stringResource(R.string.pc_mac_hint),
                error = state.macError?.let { stringResource(it) },
                keyboardType = KeyboardType.Ascii,
            )
        }
    }
}
