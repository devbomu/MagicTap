package com.magictap.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.magictap.AppContainer
import com.magictap.BuildConfig
import com.magictap.R
import com.magictap.data.WolRepository
import com.magictap.data.crypto.BackupCrypto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

enum class ExportMode { ENCRYPTED, NO_SECRET, PLAIN }

sealed interface ImportOutcome {
    data class Success(val profileCount: Int) : ImportOutcome
    data class Failure(val messageResId: Int) : ImportOutcome
}

class SettingsViewModel(private val repository: WolRepository) : ViewModel() {

    val appVersion: String = BuildConfig.VERSION_NAME

    /** Serializes (and, for [ExportMode.ENCRYPTED], encrypts) the current document. */
    suspend fun buildBackup(mode: ExportMode, password: CharArray?): String = withContext(Dispatchers.Default) {
        val data = repository.current()
        when (mode) {
            ExportMode.ENCRYPTED -> BackupCrypto.exportEncrypted(data, password ?: CharArray(0))
            ExportMode.NO_SECRET -> BackupCrypto.exportWithoutSecrets(data)
            ExportMode.PLAIN -> BackupCrypto.exportPlain(data)
        }
    }

    suspend fun applyImport(text: String, password: CharArray?, overwrite: Boolean): ImportOutcome =
        withContext(Dispatchers.Default) {
            try {
                val data = BackupCrypto.decode(text, password)
                if (overwrite) repository.replaceAll(data) else repository.merge(data)
                ImportOutcome.Success(data.profiles.size)
            } catch (e: BackupCrypto.BackupException) {
                ImportOutcome.Failure(
                    when (e.kind) {
                        BackupCrypto.BackupException.Kind.WRONG_PASSWORD -> R.string.import_fail_password
                        BackupCrypto.BackupException.Kind.UNSUPPORTED_VERSION -> R.string.import_fail_version
                        BackupCrypto.BackupException.Kind.MALFORMED -> R.string.import_fail_parse
                    },
                )
            }
        }

    fun isEncryptedBackup(text: String): Boolean = BackupCrypto.isEncrypted(text)

    companion object {
        fun factory(container: AppContainer): ViewModelProvider.Factory = viewModelFactory {
            initializer { SettingsViewModel(container.repository) }
        }
    }
}
