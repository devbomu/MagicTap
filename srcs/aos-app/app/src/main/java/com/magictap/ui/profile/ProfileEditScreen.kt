package com.magictap.ui.profile

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.view.WindowManager
import android.widget.Toast
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.magictap.R
import com.magictap.appContainer
import com.magictap.ui.AppIcons
import com.magictap.ui.components.LabeledField
import com.magictap.ui.components.PrimaryButton
import com.magictap.ui.components.SecondaryButton
import com.magictap.ui.pico.PicoConfigDialog

private val SuccessGreen = Color(0xFF2E7D32)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileEditScreen(
    profileId: String?,
    onDone: () -> Unit,
) {
    val context = LocalContext.current
    val container = remember { context.appContainer }
    val viewModel: ProfileEditViewModel =
        viewModel(factory = ProfileEditViewModel.factory(container, profileId))
    val state by viewModel.state.collectAsState()
    val clipboard = LocalClipboardManager.current
    var showPicoConfig by remember { mutableStateOf(false) }

    // The secret is visible on this screen — block screenshots / recents thumbnails (§8).
    SecureScreenEffect()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(
                            if (state.isNew) R.string.profile_edit_title_new else R.string.profile_edit_title_edit,
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
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Spacer(Modifier.height(2.dp))
            LabeledField(
                value = state.alias,
                onValueChange = viewModel::updateAlias,
                label = stringResource(R.string.profile_alias),
                placeholder = stringResource(R.string.profile_alias_hint),
                error = state.aliasError?.let { stringResource(it) },
            )
            LabeledField(
                value = state.internalHost,
                onValueChange = viewModel::updateInternalHost,
                label = stringResource(R.string.profile_internal_host),
                placeholder = stringResource(R.string.profile_internal_host_hint),
                error = state.hostError?.let { stringResource(it) },
                keyboardType = KeyboardType.Uri,
            )
            LabeledField(
                value = state.externalHost,
                onValueChange = viewModel::updateExternalHost,
                label = stringResource(R.string.profile_external_host),
                placeholder = stringResource(R.string.profile_external_host_hint),
                keyboardType = KeyboardType.Uri,
            )
            LabeledField(
                value = state.externalPort,
                onValueChange = viewModel::updateExternalPort,
                label = stringResource(R.string.profile_external_port),
                placeholder = stringResource(R.string.profile_external_port_hint),
                error = state.portError?.let { stringResource(it) },
                keyboardType = KeyboardType.Number,
            )

            SecretField(
                secret = state.secret,
                onCopy = {
                    clipboard.setText(AnnotatedString(state.secret))
                    Toast.makeText(context, R.string.profile_secret_copied, Toast.LENGTH_SHORT).show()
                },
                onRegenerate = viewModel::regenerateSecret,
            )

            ConnectionTestSection(
                internal = state.internalTest,
                external = state.externalTest,
                onTest = viewModel::runConnectionTest,
            )
            SecondaryButton(
                text = stringResource(R.string.profile_pico_setup),
                icon = AppIcons.Download,
                onClick = { showPicoConfig = true },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))
        }
    }

    if (showPicoConfig) {
        PicoConfigDialog(
            secret = state.secret,
            defaultStaticIp = state.internalHost,
            onDismiss = { showPicoConfig = false },
        )
    }
}

@Composable
private fun SecretField(secret: String, onCopy: () -> Unit, onRegenerate: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        OutlinedTextField(
            value = secret,
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(R.string.profile_secret)) },
            textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
            trailingIcon = {
                Row {
                    IconButton(onClick = onCopy) {
                        Icon(AppIcons.Copy, contentDescription = stringResource(R.string.profile_secret_copy))
                    }
                    IconButton(onClick = onRegenerate) {
                        Icon(AppIcons.Refresh, contentDescription = stringResource(R.string.profile_secret_regenerate))
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            stringResource(R.string.profile_secret_help),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 12.dp),
        )
    }
}

@Composable
private fun ConnectionTestSection(
    internal: TestState,
    external: TestState,
    onTest: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SecondaryButton(
            text = stringResource(R.string.profile_test),
            icon = AppIcons.Wifi,
            onClick = onTest,
            modifier = Modifier.fillMaxWidth(),
        )
        TestResultRow(stringResource(R.string.profile_test_internal), internal)
        TestResultRow(stringResource(R.string.profile_test_external), external)
    }
}

@Composable
private fun TestResultRow(label: String, state: TestState) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        when (state) {
            TestState.Idle -> Icon(
                AppIcons.Unchecked,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp),
            )

            TestState.Running -> CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
            is TestState.Ok -> Icon(
                AppIcons.Success,
                contentDescription = null,
                tint = SuccessGreen,
                modifier = Modifier.size(20.dp),
            )

            is TestState.Failed -> Icon(
                AppIcons.Error,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(20.dp),
            )

            TestState.SecretMismatch -> Icon(
                AppIcons.Error,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(20.dp),
            )

            TestState.ClockError -> Icon(
                AppIcons.Error,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(20.dp),
            )
        }
        Text(label, style = MaterialTheme.typography.labelLarge, modifier = Modifier.width(40.dp))
        Text(
            text = when (state) {
                TestState.Idle -> stringResource(R.string.profile_test_untested)
                TestState.Running -> stringResource(R.string.profile_test_running)
                is TestState.Ok -> stringResource(R.string.profile_test_ok, state.latencyMs, state.firmware.ifBlank { "?" })
                is TestState.Failed -> "${stringResource(R.string.profile_test_fail)} (${state.reason})"
                TestState.SecretMismatch -> stringResource(R.string.profile_test_secret_mismatch)
                TestState.ClockError -> stringResource(R.string.profile_test_clock_error)
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** Adds FLAG_SECURE while this screen is shown, then restores. */
@Composable
private fun SecureScreenEffect() {
    val view = LocalView.current
    DisposableEffect(Unit) {
        val window = view.context.findActivity()?.window
        window?.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
        onDispose { window?.clearFlags(WindowManager.LayoutParams.FLAG_SECURE) }
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
