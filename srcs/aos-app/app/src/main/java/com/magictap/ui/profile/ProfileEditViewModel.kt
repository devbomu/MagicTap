package com.magictap.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.magictap.AppContainer
import com.magictap.R
import com.magictap.data.WolRepository
import com.magictap.data.model.Profile
import com.magictap.net.HmacSigner
import com.magictap.net.PingOutcome
import com.magictap.net.VerifyOutcome
import com.magictap.net.WolClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

/** Result of a `/ping` connection test, shown per address in the editor. */
sealed interface TestState {
    data object Idle : TestState
    data object Running : TestState
    data class Ok(val latencyMs: Long, val firmware: String) : TestState
    data class Failed(val reason: String) : TestState

    /** Reachable, but the profile's secret was rejected by the Pico W's /verify. */
    data object SecretMismatch : TestState

    /** Reachable, but the Pico W's clock isn't NTP-synced (stale-timestamp rejection). */
    data object ClockError : TestState
}

data class ProfileFormState(
    val id: String,
    val isNew: Boolean,
    val loaded: Boolean = false,
    val alias: String = "",
    val internalHost: String = "",
    val externalHost: String = "",
    val externalPort: String = "18080",
    val secret: String = "",
    val aliasError: Int? = null,
    val hostError: Int? = null,
    val portError: Int? = null,
    val internalTest: TestState = TestState.Idle,
    val externalTest: TestState = TestState.Idle,
)

class ProfileEditViewModel(
    private val editingId: String?,
    private val repository: WolRepository,
    private val wolClient: WolClient,
) : ViewModel() {

    private var original: Profile? = null

    private val _state = MutableStateFlow(
        ProfileFormState(id = editingId ?: UUID.randomUUID().toString(), isNew = editingId == null),
    )
    val state = _state.asStateFlow()

    init {
        if (editingId == null) {
            _state.update { it.copy(secret = HmacSigner.newSecret(), loaded = true) }
        } else {
            viewModelScope.launch {
                val profile = repository.findProfile(editingId)
                original = profile
                _state.value = profile?.toForm() ?: _state.value.copy(loaded = true)
            }
        }
    }

    fun updateAlias(value: String) = _state.update { it.copy(alias = value, aliasError = null) }
    fun updateInternalHost(value: String) = _state.update { it.copy(internalHost = value, hostError = null, internalTest = TestState.Idle) }
    fun updateExternalHost(value: String) = _state.update { it.copy(externalHost = value, hostError = null, externalTest = TestState.Idle) }
    fun updateExternalPort(value: String) = _state.update { it.copy(externalPort = value.filter(Char::isDigit), portError = null, externalTest = TestState.Idle) }

    fun regenerateSecret() = _state.update { it.copy(secret = HmacSigner.newSecret()) }

    fun runConnectionTest() {
        testInternal()
        testExternal()
    }

    private fun testInternal() {
        val host = _state.value.internalHost.trim()
        if (host.isBlank()) {
            _state.update { it.copy(internalTest = TestState.Idle) }
            return
        }
        _state.update { it.copy(internalTest = TestState.Running) }
        viewModelScope.launch {
            _state.update { it.copy(internalTest = probe(host, INTERNAL_PORT, WolClient.TEST_INTERNAL_TIMEOUT_MS)) }
        }
    }

    private fun testExternal() {
        val host = _state.value.externalHost.trim()
        val port = _state.value.externalPort.toIntOrNull()
        if (host.isBlank() || port == null) {
            _state.update { it.copy(externalTest = TestState.Idle) }
            return
        }
        _state.update { it.copy(externalTest = TestState.Running) }
        viewModelScope.launch {
            _state.update { it.copy(externalTest = probe(host, port, WolClient.TEST_EXTERNAL_TIMEOUT_MS)) }
        }
    }

    /** Validates and persists. Invokes [onSaved] only on success. */
    fun save(onSaved: () -> Unit) {
        val s = _state.value
        val aliasError = if (s.alias.isBlank()) R.string.profile_error_alias else null
        val hostError = if (s.internalHost.isBlank() && s.externalHost.isBlank()) R.string.profile_error_host else null
        val port = s.externalPort.toIntOrNull()
        val portError = if (s.externalHost.isNotBlank() && (port == null || port !in 1..65535)) {
            R.string.profile_error_port
        } else {
            null
        }
        if (aliasError != null || hostError != null || portError != null) {
            _state.update { it.copy(aliasError = aliasError, hostError = hostError, portError = portError) }
            return
        }

        val base = original ?: Profile(id = s.id, alias = s.alias)
        val profile = base.copy(
            alias = s.alias.trim(),
            internalHost = s.internalHost.trim(),
            externalHost = s.externalHost.trim(),
            externalPort = port ?: 18080,
            internalPort = INTERNAL_PORT,
            secret = s.secret,
        )
        viewModelScope.launch {
            if (s.isNew) repository.addProfile(profile) else repository.updateProfile(profile)
            onSaved()
        }
    }

    private fun Profile.toForm() = ProfileFormState(
        id = id,
        isNew = false,
        loaded = true,
        alias = alias,
        internalHost = internalHost,
        externalHost = externalHost,
        externalPort = externalPort.toString(),
        secret = secret,
    )

    /** Pings for reachability + firmware, then /verify to confirm the profile's secret. */
    private suspend fun probe(host: String, port: Int, timeoutMs: Long): TestState {
        val ping = wolClient.ping(host, port, timeoutMs)
        if (ping is PingOutcome.Failed) return TestState.Failed(ping.reason)
        val ok = ping as PingOutcome.Ok
        return when (wolClient.verify(host, port, _state.value.secret, timeoutMs)) {
            VerifyOutcome.Rejected -> TestState.SecretMismatch
            VerifyOutcome.ClockError -> TestState.ClockError
            else -> TestState.Ok(ok.latencyMs, ok.firmware)
        }
    }

    companion object {
        private const val INTERNAL_PORT = 80

        fun factory(container: AppContainer, profileId: String?): ViewModelProvider.Factory = viewModelFactory {
            initializer { ProfileEditViewModel(profileId, container.repository, container.wolClient) }
        }
    }
}
