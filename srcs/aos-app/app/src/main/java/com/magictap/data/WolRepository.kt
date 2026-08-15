package com.magictap.data

import com.magictap.data.model.AppData
import com.magictap.data.model.Pc
import com.magictap.data.model.Profile
import com.magictap.data.store.SecureStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * Single source of truth for the app document. Exposes a reactive [data] flow for the
 * UI and suspend accessors for the widget/confirm code paths, and serializes every
 * mutation through a [Mutex] before persisting via [SecureStore].
 */
class WolRepository(private val store: SecureStore) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val mutex = Mutex()

    private val _data = MutableStateFlow(AppData())
    val data: StateFlow<AppData> = _data.asStateFlow()

    private val _ready = MutableStateFlow(false)
    /** False until the encrypted store has been read once; the UI shows a spinner meanwhile. */
    val ready: StateFlow<Boolean> = _ready.asStateFlow()

    init {
        scope.launch {
            val loaded = withContext(Dispatchers.IO) { store.load() }
            mutex.withLock {
                if (!_ready.value) {
                    _data.value = loaded
                    _ready.value = true
                }
            }
        }
    }

    /** Returns the current document, forcing the initial read if it hasn't happened yet. */
    suspend fun current(): AppData {
        if (_ready.value) return _data.value
        return mutex.withLock {
            if (!_ready.value) {
                _data.value = withContext(Dispatchers.IO) { store.load() }
                _ready.value = true
            }
            _data.value
        }
    }

    suspend fun findProfile(profileId: String): Profile? =
        current().profiles.firstOrNull { it.id == profileId }

    suspend fun findPc(profileId: String, pcId: String): Pair<Profile, Pc>? {
        val profile = findProfile(profileId) ?: return null
        val pc = profile.pcs.firstOrNull { it.id == pcId } ?: return null
        return profile to pc
    }

    // ---- Profile CRUD ----

    suspend fun addProfile(profile: Profile) = mutate { it.copy(profiles = it.profiles + profile) }

    suspend fun updateProfile(profile: Profile) = mutate { doc ->
        doc.copy(profiles = doc.profiles.map { if (it.id == profile.id) profile else it })
    }

    suspend fun deleteProfile(profileId: String) = mutate { doc ->
        doc.copy(profiles = doc.profiles.filterNot { it.id == profileId })
    }

    // ---- PC CRUD (nested under a profile) ----

    suspend fun addPc(profileId: String, pc: Pc) = mutate { doc ->
        doc.mapProfile(profileId) { it.copy(pcs = it.pcs + pc) }
    }

    suspend fun updatePc(profileId: String, pc: Pc) = mutate { doc ->
        doc.mapProfile(profileId) { profile ->
            profile.copy(pcs = profile.pcs.map { if (it.id == pc.id) pc else it })
        }
    }

    suspend fun deletePc(profileId: String, pcId: String) = mutate { doc ->
        doc.mapProfile(profileId) { profile ->
            profile.copy(pcs = profile.pcs.filterNot { it.id == pcId })
        }
    }

    // ---- Import ----

    suspend fun replaceAll(incoming: AppData) = mutate { incoming }

    suspend fun merge(incoming: AppData) = mutate { existing -> mergeDocuments(existing, incoming) }

    // ---- internals ----

    private suspend fun mutate(block: (AppData) -> AppData) {
        mutex.withLock {
            val base = if (_ready.value) _data.value else withContext(Dispatchers.IO) { store.load() }
            val next = block(base).copy(version = AppData.CURRENT_VERSION)
            withContext(Dispatchers.IO) { store.save(next) }
            _data.value = next
            _ready.value = true
        }
    }

    private fun AppData.mapProfile(profileId: String, transform: (Profile) -> Profile): AppData =
        copy(profiles = profiles.map { if (it.id == profileId) transform(it) else it })

    private fun mergeDocuments(existing: AppData, incoming: AppData): AppData {
        val merged = existing.profiles.toMutableList()
        for (inc in incoming.profiles) {
            val index = merged.indexOfFirst { it.id == inc.id }
            if (index >= 0) {
                val current = merged[index]
                merged[index] = inc.copy(
                    // Keep an existing secret when the incoming one is blank (e.g. a
                    // secret-excluded backup being merged back in).
                    secret = inc.secret.ifBlank { current.secret },
                    pcs = mergePcs(current.pcs, inc.pcs),
                )
            } else {
                merged.add(inc)
            }
        }
        return existing.copy(profiles = merged)
    }

    private fun mergePcs(current: List<Pc>, incoming: List<Pc>): List<Pc> {
        val merged = current.toMutableList()
        for (pc in incoming) {
            val index = merged.indexOfFirst { it.id == pc.id }
            if (index >= 0) merged[index] = pc else merged.add(pc)
        }
        return merged
    }
}
