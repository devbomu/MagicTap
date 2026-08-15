package com.magictap

import android.content.Context
import androidx.glance.appwidget.updateAll
import com.magictap.data.WolRepository
import com.magictap.data.store.SecureStore
import com.magictap.net.WolClient
import com.magictap.widget.ListWidget
import com.magictap.widget.SingleWidget
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch

/**
 * Tiny manual dependency container. The app has no need for a DI framework: a repository,
 * a store, and an HTTP client, all process-wide singletons, held by the [Application].
 */
class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    val store: SecureStore = SecureStore(appContext)
    val repository: WolRepository = WolRepository(store)
    val wolClient: WolClient = WolClient()

    init {
        // Refresh any placed widgets whenever the document changes (PC added, alias
        // edited, imported…). drop(1) skips the initial empty-state replay.
        CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
            repository.data.drop(1).collect {
                runCatching {
                    ListWidget().updateAll(appContext)
                    SingleWidget().updateAll(appContext)
                }
            }
        }
    }
}

/** Convenience accessor from any Context (activities, widgets, receivers). */
val Context.appContainer: AppContainer
    get() = (applicationContext as MagicTapApplication).container
