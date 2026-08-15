package com.magictap.ui.pc

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.magictap.AppContainer
import com.magictap.R
import com.magictap.data.WolRepository
import com.magictap.data.model.Pc
import com.magictap.net.MacUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

data class PcFormState(
    val id: String,
    val isNew: Boolean,
    val loaded: Boolean = false,
    val alias: String = "",
    val mac: String = "",
    val aliasError: Int? = null,
    val macError: Int? = null,
)

class PcEditViewModel(
    private val profileId: String,
    private val editingId: String?,
    private val repository: WolRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(
        PcFormState(id = editingId ?: UUID.randomUUID().toString(), isNew = editingId == null),
    )
    val state = _state.asStateFlow()

    init {
        if (editingId == null) {
            _state.update { it.copy(loaded = true) }
        } else {
            viewModelScope.launch {
                val pc = repository.findPc(profileId, editingId)?.second
                _state.value = pc?.let {
                    PcFormState(id = it.id, isNew = false, loaded = true, alias = it.alias, mac = it.mac)
                } ?: _state.value.copy(loaded = true)
            }
        }
    }

    fun updateAlias(value: String) = _state.update { it.copy(alias = value, aliasError = null) }
    fun updateMac(value: String) = _state.update { it.copy(mac = value, macError = null) }

    fun save(onSaved: () -> Unit) {
        val s = _state.value
        val aliasError = if (s.alias.isBlank()) R.string.pc_error_alias else null
        val canonicalMac = MacUtils.normalize(s.mac)
        val macError = if (canonicalMac == null) R.string.pc_error_mac else null
        if (aliasError != null || macError != null) {
            _state.update { it.copy(aliasError = aliasError, macError = macError) }
            return
        }

        val pc = Pc(id = s.id, alias = s.alias.trim(), mac = canonicalMac!!)
        viewModelScope.launch {
            if (s.isNew) repository.addPc(profileId, pc) else repository.updatePc(profileId, pc)
            onSaved()
        }
    }

    companion object {
        fun factory(container: AppContainer, profileId: String, pcId: String?): ViewModelProvider.Factory =
            viewModelFactory {
                initializer { PcEditViewModel(profileId, pcId, container.repository) }
            }
    }
}
