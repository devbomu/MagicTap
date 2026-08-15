package com.magictap.widget

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.lifecycleScope
import com.magictap.R
import com.magictap.appContainer
import com.magictap.data.model.Pc
import com.magictap.data.model.Profile
import com.magictap.ui.theme.MagicTapTheme
import com.magictap.ui.toUiMessage
import kotlinx.coroutines.launch

/**
 * The confirm step for a widget tap (design doc §7.3). A widget click is a user
 * interaction, so starting this activity from the widget's PendingIntent is exempt from
 * Android 12+ background-activity-start limits — no need to open the full app first.
 *
 * The window is transparent (see `Theme.MagicTap.Confirm`); the Compose AlertDialog is
 * the whole UI. On confirm it sends the wake, toasts the result, and finishes.
 */
class ConfirmActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val profileId = intent.getStringExtra(WidgetIntents.EXTRA_PROFILE_ID)
        val pcId = intent.getStringExtra(WidgetIntents.EXTRA_PC_ID)
        if (profileId == null || pcId == null) {
            finish()
            return
        }

        val container = appContainer

        setContent {
            MagicTapTheme {
                var target by remember { mutableStateOf<Pair<Profile, Pc>?>(null) }
                var sending by remember { mutableStateOf(false) }

                LaunchedEffect(Unit) {
                    val found = container.repository.findPc(profileId, pcId)
                    if (found == null) {
                        Toast.makeText(this@ConfirmActivity, R.string.wake_fail_nopc, Toast.LENGTH_SHORT).show()
                        finish()
                    } else {
                        target = found
                    }
                }

                target?.let { (profile, pc) ->
                    AlertDialog(
                        onDismissRequest = { if (!sending) finish() },
                        title = { Text(getString(R.string.confirm_message, pc.alias)) },
                        confirmButton = {
                            TextButton(
                                enabled = !sending,
                                onClick = {
                                    sending = true
                                    lifecycleScope.launch {
                                        val outcome = container.wolClient.wake(profile, pc)
                                        Toast.makeText(
                                            this@ConfirmActivity,
                                            outcome.toUiMessage(this@ConfirmActivity),
                                            Toast.LENGTH_SHORT,
                                        ).show()
                                        finish()
                                    }
                                },
                            ) { Text(getString(R.string.confirm_wake)) }
                        },
                        dismissButton = {
                            TextButton(enabled = !sending, onClick = { finish() }) {
                                Text(getString(R.string.action_cancel))
                            }
                        },
                    )
                }
            }
        }
    }
}
