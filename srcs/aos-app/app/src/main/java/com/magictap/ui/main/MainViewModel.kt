package com.magictap.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.magictap.AppContainer
import com.magictap.data.WolRepository
import com.magictap.data.model.Pc
import com.magictap.data.model.Profile
import com.magictap.net.WakeOutcome
import com.magictap.net.WolClient
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MainViewModel(
    private val repository: WolRepository,
    private val wolClient: WolClient,
) : ViewModel() {

    val data = repository.data
    val ready = repository.ready

    private val _selectedProfileId = MutableStateFlow<String?>(null)
    val selectedProfileId = _selectedProfileId.asStateFlow()

    private val _events = MutableSharedFlow<WakeEvent>(extraBufferCapacity = 8)
    val events = _events.asSharedFlow()

    fun selectProfile(profileId: String) {
        _selectedProfileId.value = profileId
    }

    /** Keeps the selection valid as profiles are added/removed. */
    fun syncSelection(profiles: List<Profile>) {
        val current = _selectedProfileId.value
        if (current == null || profiles.none { it.id == current }) {
            _selectedProfileId.value = profiles.firstOrNull()?.id
        }
    }

    fun wake(profile: Profile, pc: Pc) {
        viewModelScope.launch {
            val outcome = wolClient.wake(profile, pc)
            _events.tryEmit(WakeEvent(pc.alias, outcome))
        }
    }

    fun deleteProfile(profileId: String) {
        viewModelScope.launch { repository.deleteProfile(profileId) }
    }

    fun deletePc(profileId: String, pcId: String) {
        viewModelScope.launch { repository.deletePc(profileId, pcId) }
    }

    data class WakeEvent(val pcAlias: String, val outcome: WakeOutcome)

    companion object {
        fun factory(container: AppContainer): ViewModelProvider.Factory = viewModelFactory {
            initializer { MainViewModel(container.repository, container.wolClient) }
        }
    }
}
