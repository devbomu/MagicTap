package com.magictap.ui.pico

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.magictap.R
import com.magictap.ui.AppIcons
import com.magictap.ui.components.LabeledField
import com.magictap.ui.components.PrimaryButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Full-screen form that turns the phone's current Wi-Fi/network defaults plus the
 * profile's secret into a ready-to-upload config.json, exported through the system file
 * picker. The secret is carried straight from the profile — never typed or shown here.
 */
@Composable
fun PicoConfigDialog(
    secret: String,
    defaultStaticIp: String,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val suggestion = remember { suggestNetwork(context) }

    var ssid by remember { mutableStateOf(suggestion.ssid) }
    var pass by remember { mutableStateOf("") }
    var staticIp by remember { mutableStateOf(defaultStaticIp.ifBlank { suggestion.staticIpGuess }) }
    var gateway by remember { mutableStateOf(suggestion.gateway) }
    var subnet by remember { mutableStateOf(suggestion.subnet) }
    var dns by remember { mutableStateOf(suggestion.dns) }
    var duckDomain by remember { mutableStateOf("") }
    var duckToken by remember { mutableStateOf("") }
    var pendingContent by remember { mutableStateOf("") }

    val createLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json"),
    ) { uri ->
        if (uri != null) {
            scope.launch {
                withContext(Dispatchers.IO) {
                    context.contentResolver.openOutputStream(uri)?.use { it.write(pendingContent.toByteArray()) }
                }
                onDismiss()
            }
        }
    }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
            Column(Modifier.fillMaxSize().statusBarsPadding()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(AppIcons.Close, contentDescription = stringResource(R.string.action_close))
                    }
                    Text(
                        stringResource(R.string.pico_config_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        stringResource(R.string.pico_config_help),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    LabeledField(value = ssid, onValueChange = { ssid = it }, label = stringResource(R.string.pico_wifi_ssid))
                    LabeledField(
                        value = pass,
                        onValueChange = { pass = it },
                        label = stringResource(R.string.pico_wifi_pass),
                        visualTransformation = PasswordVisualTransformation(),
                    )
                    LabeledField(value = staticIp, onValueChange = { staticIp = it }, label = stringResource(R.string.pico_static_ip), keyboardType = KeyboardType.Uri)
                    LabeledField(value = gateway, onValueChange = { gateway = it }, label = stringResource(R.string.pico_gateway), keyboardType = KeyboardType.Uri)
                    LabeledField(value = subnet, onValueChange = { subnet = it }, label = stringResource(R.string.pico_subnet), keyboardType = KeyboardType.Uri)
                    LabeledField(value = dns, onValueChange = { dns = it }, label = stringResource(R.string.pico_dns), keyboardType = KeyboardType.Uri)
                    Text(
                        stringResource(R.string.pico_dns_help),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    LabeledField(value = duckDomain, onValueChange = { duckDomain = it }, label = stringResource(R.string.pico_duckdns_domain))
                    LabeledField(value = duckToken, onValueChange = { duckToken = it }, label = stringResource(R.string.pico_duckdns_token))
                    Text(
                        stringResource(R.string.pico_secret_note),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(4.dp))
                }

                Box(Modifier.fillMaxWidth().navigationBarsPadding().imePadding().padding(16.dp)) {
                    PrimaryButton(
                        text = stringResource(R.string.pico_export),
                        icon = AppIcons.Download,
                        onClick = {
                            pendingContent = PicoConfig(
                                wifiSsid = ssid.trim(),
                                wifiPass = pass,
                                staticIp = staticIp.trim(),
                                subnet = subnet.trim(),
                                gateway = gateway.trim(),
                                dns = dns.trim(),
                                httpPort = 80,
                                secret = secret,
                                duckdnsDomain = duckDomain.trim(),
                                duckdnsToken = duckToken.trim(),
                                ntpHost = "pool.ntp.org",
                            ).toJson()
                            createLauncher.launch("magictap-picoconfig.json")
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}
